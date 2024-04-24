package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.component.ArmResponseFilesProcessSingleElement;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("${darts.storage.arm.batch-size} == 0")
public class ArmResponseFilesProcessorImpl implements ArmResponseFilesProcessor {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final UserIdentity userIdentity;
    private final ArmResponseFilesProcessSingleElement armResponseFilesProcessSingleElement;
    private UserAccountEntity userAccount;

    @Override
    public void processResponseFiles() {
        initialisePreloadedObjects();
        // Fetch All records from external_object_directory table with external_location_type as 'ARM' and with status 'Arm Drop Zone'.
        List<ExternalObjectDirectoryEntity> dataSentToArm =
            externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(EodHelper.armLocation(), EodHelper.armDropZoneStatus());

        List<Integer> externalObjects = dataSentToArm.stream().map(ExternalObjectDirectoryEntity::getId).toList();
        log.info("ARM Response process found : {} records to be processed", externalObjects.size());
        for (ExternalObjectDirectoryEntity externalObjectDirectory : dataSentToArm) {
            updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armProcessingResponseFilesStatus());
        }
        int row = 1;
        for (Integer eodId : externalObjects) {
            log.info("ARM Response process about to process {} of {} rows", row++, externalObjects.size());
            armResponseFilesProcessSingleElement.processResponseFilesFor(eodId);
        }
    }

    @SuppressWarnings("java:S3655")
    private void initialisePreloadedObjects() {
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
