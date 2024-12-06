package uk.gov.hmcts.darts.datamanagement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryQueryTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.datamanagement.service.InboundTranscriptionAnnotationDeleterProcessor;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InboundTranscriptionAnnotationDeleterProcessorImpl implements InboundTranscriptionAnnotationDeleterProcessor {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final UserIdentity userIdentity;
    private final CurrentTimeHelper currentTimeHelper;
    private final UserAccountRepository userAccountRepository;
    private final EodHelper eodHelper;

    @Value("${darts.data-management.retention-period.inbound.unstructured-minimum.hours}")
    int hoursInUnstructured;

    @Override
    public List<Integer> markForDeletion(int batchSize) {
        return markForDeletion(hoursInUnstructured, batchSize);
    }

    @Override
    public List<Integer> markForDeletion(int hourBeforeCurrentDate, int batchSize) {
        OffsetDateTime lastModifiedBefore = currentTimeHelper.currentOffsetDateTime()
            .minusHours(hourBeforeCurrentDate);

        List<Integer> recordsMarkedForDeletion
            = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(EodHelper.storedStatus(),
                                                  EodHelper.storedStatus(),
                                                  EodHelper.inboundLocation(),
                                                  EodHelper.unstructuredLocation(),
                                                  lastModifiedBefore,
                                                  ExternalObjectDirectoryQueryTypeEnum.ANNOTATION_QUERY.getIndex(),
                                                  Limit.of(batchSize));

        if (recordsMarkedForDeletion.isEmpty()) {
            log.info("No records found to be marked for deletion");
        } else {
            log.debug("Identified records to be marked for deletion  {}", StringUtils.join(recordsMarkedForDeletion, ","));
            UserAccountEntity user = userIdentity.getUserAccount();
            eodHelper.updateStatus(
                EodHelper.markForDeletionStatus(),
                user,
                recordsMarkedForDeletion,
                currentTimeHelper.currentOffsetDateTime()
            );
            recordsMarkedForDeletion.stream().forEach(eodId -> log.info("Set status of unstructured EOD {} to be marked for deletion", eodId));
        }

        return recordsMarkedForDeletion;
    }
}