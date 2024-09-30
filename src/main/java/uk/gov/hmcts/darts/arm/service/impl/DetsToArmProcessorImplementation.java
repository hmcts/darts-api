package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.DetsToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItem;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItems;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;

import static uk.gov.hmcts.darts.common.util.EodHelper.isEqual;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetsToArmProcessorImplementation {

    private final ArchiveRecordService archiveRecordService;
    private final ArchiveRecordFileGenerator archiveRecordFileGenerator;
    private final DataStoreToArmHelper dataStoreToArmHelper;
    private final UserIdentity userIdentity;
    private final LogApi logApi;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final FileOperationService fileOperationService;
    private final ArmDataManagementApi armDataManagementApi;
    private final DetsToArmProcessorConfiguration detsToArmProcessorConfiguration;

    public void processDetsToArm(int taskBatchSize) {
        log.info("Started running DETS ARM Batch Push processing at: {}", OffsetDateTime.now());
        ExternalLocationTypeEntity eodSourceLocation = EodHelper.detsLocation();

        // Because the query is long-running, get all the EODs that need to be processed in one go
        List<ExternalObjectDirectoryEntity> eodsForTransfer = dataStoreToArmHelper
            .getDetsEodEntitiesToSendToArm(eodSourceLocation,
                                           EodHelper.armLocation(),
                                           taskBatchSize);

        log.info("Found {} DETS pending entities to process from source '{}'", eodsForTransfer.size(), eodSourceLocation.getDescription());
        if (!eodsForTransfer.isEmpty()) {
            //ARM has a max batch size for manifest items, so lets loop through the big list creating lots of individual batches for ARM to process separately
            List<List<ExternalObjectDirectoryEntity>> batchesForArm = ListUtils.partition(eodsForTransfer,
                                                                                          detsToArmProcessorConfiguration.getMaxArmManifestItems());
            int batchCounter = 1;
            UserAccountEntity userAccount = userIdentity.getUserAccount();
            for (List<ExternalObjectDirectoryEntity> eodsForBatch : batchesForArm) {
                log.info("Creating DETS batch {} out of {}", batchCounter++, batchesForArm.size());
                createAndSendBatchFile(eodsForBatch, userAccount);
            }
        }
        log.info("Finished running DETS ARM Batch Push processing at: {}", OffsetDateTime.now());
    }

    private void createAndSendBatchFile(List<ExternalObjectDirectoryEntity> eodsForBatch, UserAccountEntity userAccount) {
        File archiveRecordsFile = dataStoreToArmHelper.createEmptyArchiveRecordsFile(armDataManagementConfiguration.getManifestFilePrefix());
        var batchItems = new ArmBatchItems();

        for (var currentEod : eodsForBatch) {

            var batchItem = new ArmBatchItem();
            try {
                ExternalObjectDirectoryEntity armEod;
                if (isEqual(currentEod.getExternalLocationType(), EodHelper.armLocation())) {
                    //retry existing attempt that has previously failed.
                    armEod = currentEod;
                    batchItem.setArmEod(armEod);
                    dataStoreToArmHelper.updateArmEodToArmIngestionStatus(
                        currentEod, batchItem, batchItems, archiveRecordsFile, userAccount);
                } else {
                    
                    armEod = dataStoreToArmHelper.createArmEodWithArmIngestionStatus(
                        currentEod, batchItem, batchItems, archiveRecordsFile, userAccount);
                }

                String rawFilename = dataStoreToArmHelper.generateRawFilename(armEod);

                if (dataStoreToArmHelper.shouldPushRawDataToArm(batchItem)) {
                    dataStoreToArmHelper.pushRawDataAndCreateArchiveRecordIfSuccess(batchItem, rawFilename, userAccount);
                } else if (dataStoreToArmHelper.shouldAddEntryToManifestFile(batchItem)) {
                    batchItem.setArchiveRecord(archiveRecordService.generateArchiveRecordInfo(batchItem.getArmEod().getId(), rawFilename));
                }
            } catch (Exception e) {
                log.error("Unable to batch push DETS EOD {} to ARM", currentEod.getId(), e);
                dataStoreToArmHelper.recoverByUpdatingEodToFailedArmStatus(batchItem, userAccount);
            }
        }

        try {
            if (!batchItems.getSuccessful().isEmpty()) {
                dataStoreToArmHelper.writeManifestFile(batchItems, archiveRecordsFile);
                dataStoreToArmHelper.copyMetadataToArm(archiveRecordsFile);
            }
        } catch (Exception e) {
            log.error("Error during generation of DETS batch manifest file {}", archiveRecordsFile.getName(), e);
            batchItems.getSuccessful().forEach(batchItem -> dataStoreToArmHelper.recoverByUpdatingEodToFailedArmStatus(batchItem, userAccount));
            return;
        }

        for (var batchItem : batchItems.getSuccessful()) {
            dataStoreToArmHelper.updateExternalObjectDirectoryStatus(batchItem.getArmEod(), EodHelper.armDropZoneStatus(), userAccount);
            logApi.armPushSuccessful(batchItem.getArmEod().getId());
        }
    }
}
