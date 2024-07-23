package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.InboundAnnotationTranscriptionDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InboundAnnotationTranscriptionDeleterProcessorImpl implements InboundAnnotationTranscriptionDeleterProcessor {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final SystemUserHelper systemUserHelper;
    private final CurrentTimeHelper currentTimeHelper;
    private final UserAccountRepository userAccountRepository;
    private final EodHelper eodHelper;

    @Value("${darts.data-management.retention-period.inbound.arm-minimum}")
    int hoursInArm;

    public List<Integer> processDeletionIfPreceding(int batch) {
        return processDeletionIfPreceding(batch, hoursInArm);
    }

    @Override
    public List<Integer> processDeletionIfPreceding(int batch, int hourBeforeCurrentDate) {

        // if a default batch size of 0 is specified this means no batch
        Pageable pageable = null;
        if (batch > 0) {
            pageable = Pageable.ofSize(batch);
        }

        OffsetDateTime lastModifiedBefore = currentTimeHelper.currentOffsetDateTime().minus(
            hoursInArm,
            ChronoUnit.HOURS
        );

        List<Integer> recordsMarkedForDeletion
            = externalObjectDirectoryRepository
            .findMediaFileIdsIn2StorageLocationsBeforeTime(pageable,
                                                           EodHelper.storedStatus(),
                                                           EodHelper.storedStatus(),
                                                           EodHelper.inboundLocation(),
                                                           EodHelper.armLocation(),
                                                           lastModifiedBefore);

        log.debug("Identified records to be deleted  {}",  recordsMarkedForDeletion.stream().map(Object::toString));

        UserAccountEntity user = userAccountRepository.findSystemUser(systemUserHelper.findSystemUserGuid("housekeeping"));
        eodHelper.updateStatus(
            EodHelper.markForDeletionStatus(),
            user,
            recordsMarkedForDeletion,
            OffsetDateTime.now()
        );

        log.debug("Records have been marked as deleted");

        return recordsMarkedForDeletion;
    }
}