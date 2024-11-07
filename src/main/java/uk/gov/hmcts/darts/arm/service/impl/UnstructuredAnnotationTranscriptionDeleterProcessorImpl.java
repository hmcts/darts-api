package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.UnstructuredTranscriptionAndAnnotationDeleterProcessor;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
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

    private final EodHelper eodHelper;

    private final CurrentTimeHelper currentTimeHelper;

    @Value("${darts.data-management.retention-period.unstructured.arm-minimum.weeks}")
    int weeksInUnstructured;

    @Value("${darts.data-management.retention-period.inbound.arm-minimum}")
    int hoursInArm;

    public List<Integer> markForDeletion() {
        return markForDeletion(weeksInUnstructured, hoursInArm);
    }

    @Override
    public List<Integer> markForDeletion(int weeksBeforeCurrentDateInUnstructured, int hoursBeforeCurrentDateInArm) {

        OffsetDateTime lastModifiedBeforeCurrentDateForUnstructured = currentTimeHelper.currentOffsetDateTime().minus(
            weeksBeforeCurrentDateInUnstructured,
            ChronoUnit.WEEKS
        );

        OffsetDateTime lastModifiedBeforeCurrentDateForArm = currentTimeHelper.currentOffsetDateTime().minus(
            hoursBeforeCurrentDateInArm,
            ChronoUnit.HOURS
        );

        List<Integer> recordsMarkedForDeletion
            = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(EodHelper.storedStatus(),
                                                  EodHelper.storedStatus(),
                                                  EodHelper.unstructuredLocation(),
                                                  EodHelper.armLocation(),
                                                  lastModifiedBeforeCurrentDateForUnstructured,
                                                  lastModifiedBeforeCurrentDateForArm
            );

        log.debug("Identified records to be marked for deletion  {}", StringUtils.join(recordsMarkedForDeletion, ","));

        eodHelper.updateStatus(
            EodHelper.markForDeletionStatus(),
            systemUserHelper.getReferenceTo(SystemUsersEnum.UNSTRUCTURED_TRANSCRIPTION_ANNOTATION_DELETER_AUTOMATED_TASK),
            recordsMarkedForDeletion,
            currentTimeHelper.currentOffsetDateTime()
        );

        log.debug("Records have been marked for deletion");

        return recordsMarkedForDeletion;
    }
}