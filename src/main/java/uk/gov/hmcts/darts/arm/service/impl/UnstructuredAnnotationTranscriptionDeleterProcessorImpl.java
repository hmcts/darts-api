package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.UnstructuredTranscriptionAndAnnotationDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnstructuredAnnotationTranscriptionDeleterProcessorImpl implements UnstructuredTranscriptionAndAnnotationDeleterProcessor {
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private final SystemUserHelper systemUserHelper;

    private final ObjectRecordStatusRepository objectRecordStatusRepository;

    private final UserAccountRepository userAccountRepository;

    private final EodHelper eodHelper;

    @Autowired
    private CurrentTimeHelper currentTimeHelper;

    @Value("${darts.data-management.retention-period.unstructured.arm-minimum.weeks}")
    int weeksInArm;

    public List<Integer> processDeletionIfPreceding(int batch) {
        return processDeletionIfPreceding(batch, weeksInArm);
    }

    @Override
    public List<Integer> processDeletionIfPreceding(int batch, int weeksBeforeCurrentDate) {

        // if a default batch size of 0 is specified this means no batch
        Pageable pageable = null;
        if (batch > 0) {
            pageable = Pageable.ofSize(batch);
        }

        OffsetDateTime lastModifiedBefore = currentTimeHelper.currentOffsetDateTime().minus(
            weeksBeforeCurrentDate,
            ChronoUnit.WEEKS
        );

        List<Integer> recordsMarkedForDeletion
            = externalObjectDirectoryRepository
            .findMediaFileIdsIn2StorageLocationsBeforeTime(pageable,
                                                           EodHelper.storedStatus(),
                                                           EodHelper.storedStatus(),
                                                           EodHelper.unstructuredLocation(),
                                                           EodHelper.armLocation(),
                                                           lastModifiedBefore);

        log.debug("Identified records to be marked for deletion  {}",  recordsMarkedForDeletion.stream().map(Object::toString));

        UserAccountEntity user = userAccountRepository.findSystemUser(systemUserHelper.findSystemUserGuid("housekeeping"));
        eodHelper.updateStatus(
            EodHelper.markForDeletionStatus(),
            user,
            recordsMarkedForDeletion,
            OffsetDateTime.now()
        );

        log.debug("Records have been marked for deletion");

        return recordsMarkedForDeletion;
    }
}