package uk.gov.hmcts.darts.cases.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.service.CaseExpiryDeleter;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.service.DataAnonymisationService;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.task.config.CaseExpiryDeletionAutomatedTaskConfig;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class CaseExpiryDeleterImpl implements CaseExpiryDeleter {

    private final CurrentTimeHelper currentTimeHelper;
    private final DataAnonymisationService dataAnonymisationService;
    private final HearingsService hearingsService;
    private final CaseRepository caseRepository;
    private final UserIdentity userAccountService;
    private final CaseExpiryDeletionAutomatedTaskConfig config;

    @Transactional
    public void delete(Integer batchSize) {
        final UserAccountEntity userAccount = userAccountService.getUserAccount();
        OffsetDateTime maxRetentionDate = currentTimeHelper.currentOffsetDateTime()
            .minus(config.getBufferDuration());

        List<Integer> caseIds = caseRepository.findCaseIdsToBeAnonymised(maxRetentionDate, Limit.of(batchSize));
        log.info("Found {} cases to be anonymised out of a batch size {}", caseIds.size(), batchSize);
        caseIds.forEach(courtCaseId -> {
            try {
                log.info("Anonymising case with id: {} because the criteria for retention has been met.", courtCaseId);
                dataAnonymisationService.anonymiseCourtCaseById(userAccount, courtCaseId, false);
                hearingsService.removeMediaLinkToHearing(courtCaseId);
            } catch (Exception e) {
                log.error("An error occurred while anonymising case with id: {}", courtCaseId, e);
            }
        });
    }

}
