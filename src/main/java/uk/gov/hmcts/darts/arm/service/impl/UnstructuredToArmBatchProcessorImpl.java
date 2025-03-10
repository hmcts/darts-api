package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.UnstructuredToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItem;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItems;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmBatchProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.util.AsyncUtil;

import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static uk.gov.hmcts.darts.common.util.EodHelper.equalsAnyStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.isEqual;


@Slf4j
@Component
@RequiredArgsConstructor
public class UnstructuredToArmBatchProcessorImpl implements UnstructuredToArmBatchProcessor {

    private final ArchiveRecordService archiveRecordService;
    private final DataStoreToArmHelper unstructuredToArmHelper;
    private final UserIdentity userIdentity;
    private final LogApi logApi;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final UnstructuredToArmProcessorConfiguration unstructuredToArmProcessorConfiguration;

    @Override
    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
    public void processUnstructuredToArm(int taskBatchSize) {

        log.info("Started running ARM Batch Push processing at: {}", OffsetDateTime.now());

        ExternalLocationTypeEntity eodSourceLocation = EodHelper.unstructuredLocation();

        // Because the query is long-running, get all the EODs that need to be processed in one go
        List<Integer> eodsForTransfer = unstructuredToArmHelper.getEodEntitiesToSendToArm(eodSourceLocation,
                                                                                          EodHelper.armLocation(),
                                                                                          taskBatchSize);

        log.info("Found {} pending entities to process from source '{}'", eodsForTransfer.size(), eodSourceLocation.getDescription());
        if (!eodsForTransfer.isEmpty()) {
            //ARM has a max batch size for manifest items, so lets loop through the big list creating lots of individual batches for ARM to process separately
            List<List<Integer>> batchesForArm = ListUtils.partition(eodsForTransfer, unstructuredToArmProcessorConfiguration.getMaxArmManifestItems());
            AtomicInteger batchCounter = new AtomicInteger(1);
            UserAccountEntity userAccount = userIdentity.getUserAccount();
            List<Callable<Void>> tasks = batchesForArm
                .stream()
                .map(eodsForBatch -> (Callable<Void>) () -> {
                    int batchNumber = batchCounter.getAndIncrement();
                    try {
                        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = externalObjectDirectoryRepository.findAllById(eodsForBatch);
                        log.info("Starting processing batch {} out of {}", batchNumber, batchesForArm.size());
                        createAndSendBatchFile(externalObjectDirectoryEntities, userAccount);
                        log.info("Finished processing batch {} out of {}", batchNumber, batchesForArm.size());
                    } catch (Exception e) {
                        log.error("Unexpected exception when processing batch {}", batchNumber, e);
                    }
                    return null;
                })
                .toList();

            try {
                AsyncUtil.invokeAllAwaitTermination(tasks, unstructuredToArmProcessorConfiguration);
            } catch (InterruptedException e) {
                log.error("Unstructured to arm batch unexpected exception", e);
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Unstructured to arm batch unexpected exception", e);
                return;
            }
        }
        log.info("Finished running ARM Batch Push processing at: {}", OffsetDateTime.now());
    }

    private void createAndSendBatchFile(List<ExternalObjectDirectoryEntity> eodsForBatch, UserAccountEntity userAccount) {
        String archiveRecordsFileName = unstructuredToArmHelper.getArchiveRecordsFileName(armDataManagementConfiguration.getManifestFilePrefix());
        var batchItems = new ArmBatchItems();

        for (var currentEod : eodsForBatch) {
            var batchItem = new ArmBatchItem();
            try {
                ExternalObjectDirectoryEntity armEod;
                if (isEqual(currentEod.getExternalLocationType(), EodHelper.armLocation())) {
                    //retry existing attempt that has previously failed.
                    armEod = currentEod;
                    batchItem.setArmEod(armEod);
                    updateArmEodToArmIngestionStatus(currentEod, batchItem, batchItems, archiveRecordsFileName, userAccount);
                } else {
                    armEod = unstructuredToArmHelper.createArmEodWithArmIngestionStatus(currentEod, batchItem, batchItems, archiveRecordsFileName, userAccount);
                }

                String rawFilename = unstructuredToArmHelper.generateRawFilename(armEod);

                if (shouldPushRawDataToArm(batchItem)) {
                    pushRawDataAndCreateArchiveRecordIfSuccess(batchItem, rawFilename, userAccount);
                } else if (shouldAddEntryToManifestFile(batchItem)) {
                    batchItem.setArchiveRecord(archiveRecordService.generateArchiveRecordInfo(batchItem.getArmEod().getId(), rawFilename));
                }
            } catch (Exception e) {
                log.error("Unable to batch push EOD {} to ARM", currentEod.getId(), e);
                recoverByUpdatingEodToFailedArmStatus(batchItem, userAccount);
            }
        }

        try {
            if (!batchItems.getSuccessful().isEmpty()) {
                String manifestFileContents = unstructuredToArmHelper.generateManifestFileContents(batchItems, archiveRecordsFileName);
                unstructuredToArmHelper.copyMetadataToArm(manifestFileContents, archiveRecordsFileName);
            }
        } catch (Exception e) {
            log.error("Error during generation of batch manifest file {}", archiveRecordsFileName, e);
            batchItems.getSuccessful().forEach(batchItem -> recoverByUpdatingEodToFailedArmStatus(batchItem, userAccount));
            return;
        }

        for (var batchItem : batchItems.getItems()) {
            if (batchItem.isRawFilePushNotNeededOrSuccessfulWhenNeeded() && batchItem.getArchiveRecord() != null) {
                unstructuredToArmHelper.updateExternalObjectDirectoryStatus(batchItem.getArmEod(), EodHelper.armDropZoneStatus(), userAccount);
                logApi.armPushSuccessful(batchItem.getArmEod().getId());
            } else {
                recoverByUpdatingEodToFailedArmStatus(batchItem, userAccount);
            }
        }
    }

    private void updateArmEodToArmIngestionStatus(ExternalObjectDirectoryEntity armEod, ArmBatchItem batchItem, ArmBatchItems batchItems,
                                                  String archiveRecordsFileName, UserAccountEntity userAccount) {
        var matchingEntity = unstructuredToArmHelper.getExternalObjectDirectoryEntity(armEod, EodHelper.unstructuredLocation(), EodHelper.storedStatus());
        if (matchingEntity.isPresent()) {
            batchItem.setSourceEod(matchingEntity.get());
            batchItems.add(batchItem);
            armEod.setManifestFile(archiveRecordsFileName);
            unstructuredToArmHelper.incrementTransferAttempts(armEod);
            unstructuredToArmHelper.updateExternalObjectDirectoryStatus(armEod, EodHelper.armIngestionStatus(), userAccount);
        } else {
            log.error("Unable to find matching external object directory {} for manifest {}", armEod.getId(), archiveRecordsFileName);
            unstructuredToArmHelper.updateExternalObjectDirectoryFailedTransferAttempts(armEod, userAccount);
            throw new RuntimeException(MessageFormat.format("Unable to find matching external object directory for {0}", armEod.getId()));
        }
    }

    private boolean shouldPushRawDataToArm(ArmBatchItem batchItem) {
        return equalsAnyStatus(batchItem.getPreviousStatus(), EodHelper.armIngestionStatus(), EodHelper.failedArmRawDataStatus());
    }

    private void pushRawDataAndCreateArchiveRecordIfSuccess(ArmBatchItem batchItem, String rawFilename, UserAccountEntity userAccount) {
        log.info("Start of batch ARM Push processing for EOD {} running at: {}", batchItem.getArmEod().getId(), OffsetDateTime.now());
        boolean copyRawDataToArmSuccessful = unstructuredToArmHelper.copyUnstructuredRawDataToArm(
            batchItem.getSourceEod(),
            batchItem.getArmEod(),
            rawFilename,
            batchItem.getPreviousStatus(),
            userAccount
        );

        if (copyRawDataToArmSuccessful) {
            batchItem.setRawFilePushSuccessful(true);
            var archiveRecord = archiveRecordService.generateArchiveRecordInfo(batchItem.getArmEod().getId(), rawFilename);
            batchItem.setArchiveRecord(archiveRecord);
        } else {
            batchItem.setRawFilePushSuccessful(false);
            batchItem.undoManifestFileChange();
            unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodHelper.failedArmRawDataStatus(), userAccount);
        }
    }

    private boolean shouldAddEntryToManifestFile(ArmBatchItem batchItem) {
        return equalsAnyStatus(batchItem.getPreviousStatus(), EodHelper.failedArmManifestFileStatus(), EodHelper.armResponseManifestFailedStatus());
    }

    @SuppressWarnings({"PMD.ConfusingTernary"})
    private void recoverByUpdatingEodToFailedArmStatus(ArmBatchItem batchItem, UserAccountEntity userAccount) {
        if (batchItem.getArmEod() != null) {
            logApi.armPushFailed(batchItem.getArmEod().getId());
            batchItem.undoManifestFileChange();
            if (!batchItem.isRawFilePushNotNeededOrSuccessfulWhenNeeded()) {
                unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodHelper.failedArmRawDataStatus(), userAccount);
            } else {
                unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodHelper.failedArmManifestFileStatus(),
                                                                                    userAccount);
            }
        }
    }

}