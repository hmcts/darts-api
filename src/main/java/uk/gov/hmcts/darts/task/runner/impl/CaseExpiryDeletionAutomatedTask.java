package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.service.DataAnonymisationService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

@Component
@ConditionalOnExpression(
    value = "#{T(java.lang.Boolean).parseBoolean(T(uk.gov.hmcts.darts.task.api.AutomatedTaskName).CASE_EXPIRY_DELETION_TASK_NAME.getConditionalOnSpEL())}"
)
@Slf4j
public class CaseExpiryDeletionAutomatedTask
    extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final CurrentTimeHelper currentTimeHelper;
    private final DataAnonymisationService dataAnonymisationService;
    private final CaseRepository caseRepository;

    public CaseExpiryDeletionAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                           AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                           CurrentTimeHelper currentTimeHelper,
                                           CaseRepository caseRepository,
                                           LogApi logApi, LockService lockService,
                                           DataAnonymisationService dataAnonymisationService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.currentTimeHelper = currentTimeHelper;
        this.caseRepository = caseRepository;
        this.dataAnonymisationService = dataAnonymisationService;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.CASE_EXPIRY_DELETION_TASK_NAME;
    }

    @Override
    @Transactional
    public void runTask() {
        caseRepository.findCasesToBeAnonymized(currentTimeHelper.currentOffsetDateTime(), Limit.of(getAutomatedTaskBatchSize()))
            .forEach(courtCase -> {
                log.info("Anonymising case with id: {} because the criteria for retention has been met.", courtCase.getId());
                dataAnonymisationService.anonymizeCourtCaseEntity(courtCase);
            });
    }
}
