package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.service.DataAnonymisationService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.CaseExpiryDeletionAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.OffsetDateTime;

@Component
@ConditionalOnProperty(
    value = "darts.automated.task.case-expiry-deletion.enabled",
    havingValue = "true"
)
@Slf4j
public class CaseExpiryDeletionAutomatedTask
    extends AbstractLockableAutomatedTask<CaseExpiryDeletionAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final CurrentTimeHelper currentTimeHelper;
    private final DataAnonymisationService dataAnonymisationService;
    private final CaseRepository caseRepository;
    private final UserIdentity userAccountService;

    public CaseExpiryDeletionAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                           CaseExpiryDeletionAutomatedTaskConfig automatedTaskConfigurationProperties,
                                           CurrentTimeHelper currentTimeHelper,
                                           CaseRepository caseRepository,
                                           LogApi logApi, LockService lockService,
                                           DataAnonymisationService dataAnonymisationService, UserIdentity userAccountService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.currentTimeHelper = currentTimeHelper;
        this.caseRepository = caseRepository;
        this.dataAnonymisationService = dataAnonymisationService;
        this.userAccountService = userAccountService;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.CASE_EXPIRY_DELETION_TASK_NAME;
    }

    @Override
    public void runTask() {
        final UserAccountEntity userAccount = userAccountService.getUserAccount();
        OffsetDateTime maxRetentionDate = currentTimeHelper.currentOffsetDateTime()
            .minus(getConfig().getBufferDuration());

        caseRepository.findCaseIdsToBeAnonymised(maxRetentionDate, Limit.of(getAutomatedTaskBatchSize()))
            .forEach(courtCaseId -> {
                try {
                    log.info("Anonymising case with id: {} because the criteria for retention has been met.", courtCaseId);
                    dataAnonymisationService.anonymiseCourtCaseById(userAccount, courtCaseId);
                } catch (Exception e) {
                    log.error("An error occurred while anonymising case with id: {}", courtCaseId, e);
                }
            });
    }
}
