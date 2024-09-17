package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
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
    private final AuditApi auditApi;

    public CaseExpiryDeletionAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                           AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                           CurrentTimeHelper currentTimeHelper,
                                           UserIdentity userIdentity,
                                           CaseRepository caseRepository,
                                           AuditApi auditApi,
                                           LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.currentTimeHelper = currentTimeHelper;
        this.userIdentity = userIdentity;
        this.caseRepository = caseRepository;
        this.auditApi = auditApi;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.CASE_EXPIRY_DELETION_TASK_NAME;
    }

    @Override
    @Transactional
    public void runTask() {
        final UserAccountEntity userAccount = userIdentity.getUserAccount();
        final List<CourtCaseEntity> courtCaseEntities = new ArrayList<>();

        caseRepository.findCasesToBeAnonymized(currentTimeHelper.currentOffsetDateTime(),
                                               Limit.of(getAutomatedTaskBatchSize()))
            .forEach(courtCase -> {
                log.info("Anonymising case with id: {} because the criteria for retention has been met.", courtCase.getId());
                courtCase.anonymize(userAccount);
                auditApi.record(AuditActivity.CASE_EXPIRED, userAccount, courtCase);

                courtCaseEntities.add(courtCase);
            });
        //This also saves defendant, defence and prosecutor entities
        caseRepository.saveAll(courtCaseEntities);
    }
}
