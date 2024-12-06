package uk.gov.hmcts.darts.arm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.blobs.ArmResponseBatchData;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseInvalidLineRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.util.Optional;


@Slf4j
@Component
public class DetsToArmBatchProcessResponseFilesImpl extends AbstractArmBatchProcessResponseFiles {

    private final DetsDataManagementConfiguration configuration;

    public DetsToArmBatchProcessResponseFilesImpl(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                                  ArmDataManagementApi armDataManagementApi, FileOperationService fileOperationService,
                                                  ArmDataManagementConfiguration armDataManagementConfiguration,
                                                  ObjectMapper objectMapper, UserIdentity userIdentity, CurrentTimeHelper currentTimeHelper,
                                                  ExternalObjectDirectoryService externalObjectDirectoryService,
                                                  LogApi logApi, DetsDataManagementConfiguration configuration,
                                                  ObjectStateRecordRepository objectStateRecordRepository) {
        super(externalObjectDirectoryRepository,
              objectStateRecordRepository,
              armDataManagementApi,
              fileOperationService,
              armDataManagementConfiguration,
              objectMapper,
              userIdentity,
              currentTimeHelper,
              externalObjectDirectoryService,
              logApi);
        this.configuration = configuration;
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
                                                         String objectChecksum,
                                                         UserAccountEntity userAccount) {
        super.onUploadFileChecksumValidationSuccess(batchUploadFileFilenameProcessor,
                                                    armResponseBatchData,
                                                    armResponseUploadFileRecord,
                                                    armEod,
                                                    objectChecksum,
                                                    userAccount);

        getObjectStateRecord(armEod.getId())
            .ifPresent(osr ->
                           updateOsrFileIngestStatusToSuccess(batchUploadFileFilenameProcessor, armResponseBatchData, objectChecksum, osr)
            );
    }

    @Override
    protected void onUploadFileChecksumValidationFailure(ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                                         ExternalObjectDirectoryEntity externalObjectDirectory,
                                                         String objectChecksum,
                                                         UserAccountEntity userAccount) {
        super.onUploadFileChecksumValidationFailure(armResponseUploadFileRecord, externalObjectDirectory, objectChecksum, userAccount);

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
                                                ExternalObjectDirectoryEntity externalObjectDirectory,
                                                UserAccountEntity userAccount) {
        super.processUploadFileDataFailure(armResponseUploadFileRecord, uploadFileFilenameProcessor, externalObjectDirectory, userAccount);
        getObjectStateRecord(externalObjectDirectory.getId())
            .ifPresent(osr -> updateOsrIngestStatusToFailure(osr, armResponseUploadFileRecord.getExceptionDescription()));
    }

    @Override
    protected void processInvalidLineFileActions(ArmResponseInvalidLineRecord armResponseInvalidLineRecord,
                                                 ExternalObjectDirectoryEntity externalObjectDirectory,
                                                 UserAccountEntity userAccount) {
        //tested
        super.processInvalidLineFileActions(armResponseInvalidLineRecord, externalObjectDirectory, userAccount);
        getObjectStateRecord(externalObjectDirectory.getId())
            .ifPresent(osr -> updateOsrIngestStatusToFailure(osr, armResponseInvalidLineRecord.getExceptionDescription()));
    }


    private Optional<ObjectStateRecordEntity> getObjectStateRecord(int armEod) {
        Optional<ObjectStateRecordEntity> osrOptional = objectStateRecordRepository.findByArmEodId(String.valueOf(armEod));
        if (osrOptional.isEmpty()) {
            log.error("Object State Record not found for Arm EOD {}", armEod);
        }
        return osrOptional;
    }

    private void updateOsrResponseReceivedAttributes(ObjectStateRecordEntity osr) {
        osr.setFlagRspnRecvdFromArml(true);
        osr.setDateRspnRecvdFromArml(timeHelper.currentOffsetDateTime());
        objectStateRecordRepository.save(osr);
    }

    private void updateOsrFileIngestStatusToSuccess(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor,
                                                    ArmResponseBatchData armResponseBatchData,
                                                    String objectChecksum,
                                                    ObjectStateRecordEntity osr) {
        osr.setFlagFileIngestStatus(true);
        osr.setDateFileIngestToArm(timeHelper.currentOffsetDateTime());
        osr.setMd5FileIngestToArm(objectChecksum);
        osr.setIdResponseFile(batchUploadFileFilenameProcessor.getBatchMetadataFilename());
        osr.setIdResponseCrFile(armResponseBatchData.getCreateRecordFilenameProcessor().getCreateRecordFilename());
        osr.setIdResponseUfFile(armResponseBatchData.getUploadFileFilenameProcessor().getUploadFileFilename());
        // clearing out any existing error messages
        osr.setObjectStatus(null);
        objectStateRecordRepository.save(osr);
    }

    private void updateOsrIngestStatusToFailure(ObjectStateRecordEntity osr,
                                                String objectStatus) {
        osr.setFlagFileIngestStatus(false);
        osr.setDateFileIngestToArm(timeHelper.currentOffsetDateTime());
        osr.setObjectStatus(objectStatus);
        objectStateRecordRepository.save(osr);
    }
}