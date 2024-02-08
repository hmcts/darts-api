package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.component.ArmResponseFilesProcessSingleElement;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_ARM_RESPONSE_PROCESSING;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmResponseFilesProcessorImpl implements ArmResponseFilesProcessor {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final UserIdentity userIdentity;
    private final ArmResponseFilesProcessSingleElement armResponseFilesProcessSingleElement;

    private ObjectRecordStatusEntity armDropZoneStatus;
    private ObjectRecordStatusEntity armProcessingResponseFilesStatus;
    private ObjectRecordStatusEntity armResponseProcessingFailed;
    private UserAccountEntity userAccount;

    @Override
    public void processResponseFiles() {
        initialisePreloadedObjects();
        // Fetch All records from external Object Directory table with external_location_type as 'ARM' and status with 'ARM dropzone'.
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.ARM.getId());

        List<ExternalObjectDirectoryEntity> dataSentToArm =
                externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(armLocation, armDropZoneStatus);
        if (CollectionUtils.isNotEmpty(dataSentToArm)) {
            List<Integer> externalObjects = dataSentToArm.stream().map(ExternalObjectDirectoryEntity::getId).toList();
            log.info("ARM Response process found : {} records to be processed", externalObjects.size());
            for (ExternalObjectDirectoryEntity externalObjectDirectory : dataSentToArm) {
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armProcessingResponseFilesStatus);
            }
            int row = 1;
            for (Integer eodId : externalObjects) {
                log.info("ARM Response process about to process {} of {} rows", row++, externalObjects.size());
                armResponseFilesProcessSingleElement.processResponseFilesFor(eodId);
            }
        } else {
            log.info("ARM Response process unable to find any records to process");
        }
    }

    @SuppressWarnings("java:S3655")
    private void initialisePreloadedObjects() {
        armDropZoneStatus = objectRecordStatusRepository.findById(ARM_DROP_ZONE.getId()).get();
        armProcessingResponseFilesStatus = objectRecordStatusRepository.findById(ARM_PROCESSING_RESPONSE_FILES.getId()).get();
        armResponseProcessingFailed = objectRecordStatusRepository.findById(FAILURE_ARM_RESPONSE_PROCESSING.getId()).get();

        userAccount = userIdentity.getUserAccount();
    }

    private void updateExternalObjectDirectoryStatus(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                     ObjectRecordStatusEntity objectRecordStatus) {
        log.info(
                "ARM Push updating ARM status from {} to {} for ID {}",
                externalObjectDirectory.getStatus().getDescription(),
                objectRecordStatus.getDescription(),
                externalObjectDirectory.getId()
        );
        externalObjectDirectory.setStatus(objectRecordStatus);
        externalObjectDirectory.setLastModifiedBy(userAccount);
        externalObjectDirectory.setLastModifiedDateTime(OffsetDateTime.now());
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
    }

}
