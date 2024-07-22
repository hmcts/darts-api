package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.UnstructuredTranscriptionAndAnnotationDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

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

    @Autowired
    private CurrentTimeHelper currentTimeHelper;

    @Value("${darts.data-management.retention-period.unstructured.arm-minimum.weeks}")
    int weeksInArm;

    public List<Integer> processDeletionIfPreceding(int batch) {
        return processDeletionIfPreceding(batch, weeksInArm);
    }

    @Override
    public List<Integer> processDeletionIfPreceding(int batch, int weeksBeforeCurrentDate) {
        OffsetDateTime lastModifiedBefore = currentTimeHelper.currentOffsetDateTime().minus(
            weeksBeforeCurrentDate,
            ChronoUnit.WEEKS
        );

        List<Integer> armRecordToBeMarkedForDeletion
            = externalObjectDirectoryRepository
            .findAllArmMediaBeforeOrEqualDate(Pageable.ofSize(batch), ExternalLocationTypeEnum.UNSTRUCTURED.getId(), lastModifiedBefore);

        ObjectRecordStatusEntity status = objectRecordStatusRepository.getReferenceById(ObjectRecordStatusEnum.MARKED_FOR_DELETION.getId());

        log.debug("Identified records to be deleted  {}",  armRecordToBeMarkedForDeletion.stream().map(Object::toString));
        externalObjectDirectoryRepository.updateStatusAndUserOfObjectDirectory(armRecordToBeMarkedForDeletion, status,
                                                                               systemUserHelper.getHousekeepingUser());
        log.debug("Records have been marked as deleted");

        return armRecordToBeMarkedForDeletion;
    }
}