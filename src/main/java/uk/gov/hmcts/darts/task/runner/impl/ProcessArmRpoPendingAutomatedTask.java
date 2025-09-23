package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ProcessArmRpoPendingAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;

@Component
@ConditionalOnProperty(
    value = "darts.automated.task.process-e2e-arm-rpo-pending.process-e2e-arm-rpo",
    havingValue = "false"
)
@Slf4j
public class ProcessArmRpoPendingAutomatedTask
    extends AbstractLockableAutomatedTask<ProcessArmRpoPendingAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final Duration armRpoDuration;
    private final UserIdentity userIdentity;

    protected ProcessArmRpoPendingAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                ProcessArmRpoPendingAutomatedTaskConfig automatedTaskConfigurationProperties,
                                                LogApi logApi, LockService lockService,
                                                ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                                CurrentTimeHelper currentTimeHelper,
                                                UserIdentity userIdentity,
                                                @Value("${darts.automated.task.process-arm-rpo-pending.arm-rpo-duration}")
                                                Duration armRpoDuration) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.armRpoDuration = armRpoDuration;
        this.currentTimeHelper = currentTimeHelper;
        this.userIdentity = userIdentity;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.PROCESS_ARM_RPO_PENDING;
    }

    @Override
    protected void runTask() {
        Integer batchSize = this.getAutomatedTaskBatchSize();
        log.info("Processing {} arm rpo pending records", batchSize);
        this.externalObjectDirectoryRepository.updateByStatusEqualsAndInputUploadProcessedTsBefore(
            EodHelper.armRpoPendingStatus(),
            this.currentTimeHelper.currentOffsetDateTime().minus(this.armRpoDuration),
            EodHelper.storedStatus(),
            userIdentity.getUserAccount().getId(),
            Limit.of(batchSize)
        );
    }
}
