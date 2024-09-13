package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingAutomatedTask;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(
    value = "darts.automated.task.expiry-deletion.enabled",
    havingValue = "true"
)
@Slf4j
public class CaseExpiryDeletionAutomatedTask
    extends AbstractLockableAutomatedTask
    implements AutoloadingAutomatedTask, AutoloadingManualTask {

    private final CurrentTimeHelper currentTimeHelper;
    private final UserIdentity userIdentity;
    private final CaseRepository caseRepository;

    public CaseExpiryDeletionAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                           AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                           CurrentTimeHelper currentTimeHelper,
                                           UserIdentity userIdentity,
                                           CaseRepository caseRepository,
                                           LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.currentTimeHelper = currentTimeHelper;
        this.userIdentity = userIdentity;
        this.caseRepository = caseRepository;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.CASE_EXPIRY_DELETION_TASK_NAME;
    }

    @Override
    @Transactional
    protected void runTask() {
        final UUID uuid = UUID.randomUUID();
        final UserAccountEntity userAccount = userIdentity.getUserAccount();
        final List<CourtCaseEntity> courtCaseEntities = new ArrayList<>();


        caseRepository.findCasesToBeAnonymized(currentTimeHelper.currentOffsetDateTime(), getAutomatedTaskBatchSize())
            .forEach(courtCase -> {
                log.info("Anonymising case with id: {} using uuid: {} because the criteria for retention has been met.",
                         courtCase.getId(), uuid);
                courtCase.anonymize(userAccount, uuid);
                courtCaseEntities.add(courtCase);
            });
        //This also saves defendant, defence and prosecutor entities
        caseRepository.saveAll(courtCaseEntities);
    }
}
