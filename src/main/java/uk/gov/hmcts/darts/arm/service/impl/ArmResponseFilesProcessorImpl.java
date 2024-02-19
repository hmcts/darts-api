package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.component.ArmResponseFilesProcessSingleElement;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES;

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
    private UserAccountEntity userAccount;

    @Override
    public void processResponseFiles() {
        initialisePreloadedObjects();
        // Fetch All records from external Object Directory table with external_location_type as 'ARM' and status with 'ARM dropzone'.
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(ARM.getId());
        List<ExternalObjectDirectoryEntity> dataSentToArm =
            externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(armLocation, armDropZoneStatus);

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
    }

    @SuppressWarnings("java:S3655")
    private void initialisePreloadedObjects() {
        armDropZoneStatus = objectRecordStatusRepository.findById(ARM_DROP_ZONE.getId()).get();
        armProcessingResponseFilesStatus = objectRecordStatusRepository.findById(ARM_PROCESSING_RESPONSE_FILES.getId()).get();

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
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
    }

}
