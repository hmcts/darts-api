package uk.gov.hmcts.darts.arm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.blobs.ArmResponseBatchData;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseInvalidLineRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.audio.deleter.impl.dets.ExternalDetsDataStoreDeleter;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.util.List;
import java.util.Optional;


@Slf4j
public class DetsToArmBatchProcessResponseFilesImpl extends AbstractArmBatchProcessResponseFiles {

    private final DetsDataManagementConfiguration configuration;
    private final ObjectStateRecordRepository osrRepository;
    private final ExternalDetsDataStoreDeleter detsDataStoreDeleter;

    public DetsToArmBatchProcessResponseFilesImpl(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                                  ArmDataManagementApi armDataManagementApi, FileOperationService fileOperationService,
                                                  ArmDataManagementConfiguration armDataManagementConfiguration,
                                                  ObjectMapper objectMapper, UserIdentity userIdentity, CurrentTimeHelper currentTimeHelper,
                                                  ExternalObjectDirectoryService externalObjectDirectoryService, Integer batchSize,
                                                  LogApi logApi, DetsDataManagementConfiguration configuration,
                                                  ObjectStateRecordRepository osrRepository,
                                                  ExternalDetsDataStoreDeleter detsDataStoreDeleter) {
        super(externalObjectDirectoryRepository,
              armDataManagementApi,
              fileOperationService,
              armDataManagementConfiguration,
              objectMapper,
              userIdentity,
              currentTimeHelper,
              externalObjectDirectoryService,
              batchSize,
              logApi);
        this.configuration = configuration;
        this.osrRepository = osrRepository;
        this.detsDataStoreDeleter = detsDataStoreDeleter;
    }

    @Override
    public String getManifestFilePrefix() {
        return configuration.getDetsManifestFilePrefix();
    }

    @Override
    protected void preProcessResponseFilesActions(int armEodId) {
        super.preProcessResponseFilesActions(armEodId);
        getObjectStateRecord(armEodId)
            .ifPresent(this::updateOsrResponseReceivedAttributes);
    }

    @Override
    protected void onUploadFileChecksumValidationSuccess(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor,
                                                         ArmResponseBatchData armResponseBatchData,
                                                         ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                                         ExternalObjectDirectoryEntity armEod,
                                                         String objectChecksum) {
        super.onUploadFileChecksumValidationSuccess(batchUploadFileFilenameProcessor,
                                                    armResponseBatchData,
                                                    armResponseUploadFileRecord,
                                                    armEod,
                                                    objectChecksum);

        getObjectStateRecord(armEod.getId()).ifPresent(osr -> {
            updateOsrFileIngestStatusToSuccess(batchUploadFileFilenameProcessor, armResponseBatchData, objectChecksum, osr);
            deleteBlobDataAndEod(armEod, osr);
        });
    }

    @Override
    protected void onUploadFileChecksumValidationFailure(ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                                         ExternalObjectDirectoryEntity externalObjectDirectory,
                                                         String objectChecksum) {
        super.onUploadFileChecksumValidationFailure(armResponseUploadFileRecord, externalObjectDirectory, objectChecksum);

        getObjectStateRecord(externalObjectDirectory.getId()).ifPresent(osr -> {
            String checksumValidationFailedMessage = String.format("External object id %s checksum differs. Arm checksum: %s Object Checksum: %s",
                                                                   externalObjectDirectory.getId(),
                                                                   armResponseUploadFileRecord.getMd5(), objectChecksum);
            updateOsrIngestStatusToFailure(osr, checksumValidationFailedMessage);
        });
    }

    @Override
    protected void processUploadFileDataFailure(ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                                UploadFileFilenameProcessor uploadFileFilenameProcessor,
                                                ExternalObjectDirectoryEntity externalObjectDirectory) {
        super.processUploadFileDataFailure(armResponseUploadFileRecord, uploadFileFilenameProcessor, externalObjectDirectory);
        getObjectStateRecord(externalObjectDirectory.getId())
            .ifPresent(osr -> updateOsrIngestStatusToFailure(osr, armResponseUploadFileRecord.getExceptionDescription()));
    }

    @Override
    protected void processInvalidLineFileActions(ArmResponseInvalidLineRecord armResponseInvalidLineRecord,
                                                 ExternalObjectDirectoryEntity externalObjectDirectory) {
        //tested
        super.processInvalidLineFileActions(armResponseInvalidLineRecord, externalObjectDirectory);
        getObjectStateRecord(externalObjectDirectory.getId())
            .ifPresent(osr -> updateOsrIngestStatusToFailure(osr, armResponseInvalidLineRecord.getExceptionDescription()));
    }


    private Optional<ObjectStateRecordEntity> getObjectStateRecord(int armEod) {
        Optional<ObjectStateRecordEntity> osrOptional = osrRepository.findByArmEodId(String.valueOf(armEod));
        if (osrOptional.isEmpty()) {
            log.error("Object State Record not found for Arm EOD {}", armEod);
        }
        return osrOptional;
    }

    private void updateOsrResponseReceivedAttributes(ObjectStateRecordEntity osr) {
        osr.setFlagRspnRecvdFromArml(true);
        osr.setDateRspnRecvdFromArml(timeHelper.currentOffsetDateTime());
        osrRepository.save(osr);
    }

    private void deleteBlobDataAndEod(ExternalObjectDirectoryEntity armEod, ObjectStateRecordEntity osr) {
        List<ExternalObjectDirectoryEntity> detsEods = externalObjectDirectoryRepository.findExternalObjectDirectoryByLocation(
            EodHelper.detsLocation(),
            armEod.getMedia(),
            armEod.getTranscriptionDocumentEntity(),
            armEod.getAnnotationDocumentEntity(),
            armEod.getCaseDocument());

        if (detsEods.size() == 1) {
            boolean deleted = detsDataStoreDeleter.delete(detsEods.getFirst());
            if (deleted) {
                osr.setFlagFileDetsCleanupStatus(true);
                osr.setDateFileDetsCleanup(timeHelper.currentOffsetDateTime());
                osrRepository.save(osr);
            } else {
                String errorMessage = String.format("Unable to delete DETS blob for ARM EDO %s", armEod.getId());
                log.error(errorMessage);
                updateOsrIngestStatusToFailure(osr, errorMessage);
            }
        } else {
            String errorMessage = String.format("Unable to delete DETS blob because either no DETS EOD for ARM EDO %s found or more than one DETS EOD found", armEod.getId());
            log.error(errorMessage);
            updateOsrIngestStatusToFailure(osr, errorMessage);
        }
    }

    private void updateOsrFileIngestStatusToSuccess(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor,
                                                    ArmResponseBatchData armResponseBatchData,
                                                    String objectChecksum,
                                                    ObjectStateRecordEntity osr) {
        osr.setFlagFileIngestStatus(true);
        osr.setDateFileIngestToArm(timeHelper.currentOffsetDateTime());
        osr.setMd5FileIngestToArm(objectChecksum);
        osr.setIdResponseFile(batchUploadFileFilenameProcessor.getInputUploadFilename());
        osr.setIdResponseCrFile(armResponseBatchData.getCreateRecordFilenameProcessor().getCreateRecordFilename());
        osr.setIdResponseUfFile(armResponseBatchData.getUploadFileFilenameProcessor().getUploadFileFilename());
        osrRepository.save(osr);
    }

    private void updateOsrIngestStatusToFailure(ObjectStateRecordEntity osr,
                                                String objectStatus) {
        osr.setFlagFileIngestStatus(false);
        osr.setDateFileIngestToArm(timeHelper.currentOffsetDateTime());
        osr.setObjectStatus(objectStatus);
        osrRepository.save(osr);
    }
}