package uk.gov.hmcts.darts.datamanagement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryQueryTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.datamanagement.service.InboundTranscriptionAnnotationDeleterProcessor;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InboundTranscriptionAnnotationDeleterProcessorImpl implements InboundTranscriptionAnnotationDeleterProcessor {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final SystemUserHelper systemUserHelper;
    private final CurrentTimeHelper currentTimeHelper;
    private final UserAccountRepository userAccountRepository;
    private final EodHelper eodHelper;

    @Value("${darts.data-management.retention-period.inbound.unstructured-minimum.hours}")
    int hoursInUnstructured;

    public List<Integer> markForDeletion(int batchSize) {
        return markForDeletion(hoursInUnstructured, batchSize);
    }

    @Override
    public List<Integer> markForDeletion(int hourBeforeCurrentDate, int batchSize) {
        OffsetDateTime lastModifiedBefore = currentTimeHelper.currentOffsetDateTime().minus(
            hourBeforeCurrentDate,
            ChronoUnit.HOURS
        );

        List<Integer> recordsMarkedForDeletion
            = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(EodHelper.storedStatus(),
                                                  EodHelper.storedStatus(),
                                                  EodHelper.inboundLocation(),
                                                  EodHelper.unstructuredLocation(),
                                                  lastModifiedBefore,
                                                  ExternalObjectDirectoryQueryTypeEnum.ANNOTATION_QUERY.getIndex(),
                                                  Limit.of(batchSize));

        log.debug("Identified records to be marked for deletion  {}", StringUtils.join(recordsMarkedForDeletion, ","));

        UserAccountEntity user = systemUserHelper.getReferenceTo(SystemUsersEnum.INBOUND_TRANSCRIPTION_ANNOTATION_DELETER_AUTOMATED_TASK);
        eodHelper.updateStatus(
            EodHelper.markForDeletionStatus(),
            user,
            recordsMarkedForDeletion,
            currentTimeHelper.currentOffsetDateTime()
        );

        recordsMarkedForDeletion.stream().forEach(eodId -> log.info("Set status of unstructured EOD {} to be marked for deletion", eodId));

        return recordsMarkedForDeletion;
    }
}