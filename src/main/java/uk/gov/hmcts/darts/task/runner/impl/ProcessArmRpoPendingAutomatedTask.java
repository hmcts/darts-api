package uk.gov.hmcts.darts.task.runner.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.ObjectRecordStatusService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;

@Component
@ConditionalOnProperty(
    value = "darts.automated.task.process-e2e-arm-rpo",
    havingValue = "false"
)
public class ProcessArmRpoPendingAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusService objectRecordStatusService;
    private final CurrentTimeHelper currentTimeHelper;
    private final Duration armRpoDuration;

    protected ProcessArmRpoPendingAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                LogApi logApi, LockService lockService,
                                                ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                                ObjectRecordStatusService objectRecordStatusService,
                                                CurrentTimeHelper currentTimeHelper,
                                                @Value("${darts.automated.task.arm-rpo-duration}")
                                                Duration armRpoDuration) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.objectRecordStatusService = objectRecordStatusService;
        this.armRpoDuration = armRpoDuration;
        this.currentTimeHelper = currentTimeHelper;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.PROCESS_ARM_RPO_PENDING;
    }

    @Override
    protected void runTask() {
        this.externalObjectDirectoryRepository.updateByStatusEqualsAndDataIngestionTsBefore(
            this.objectRecordStatusService.getObjectRecordStatusEntity(ObjectRecordStatusEnum.ARM_RPO_PENDING),
            this.currentTimeHelper.currentOffsetDateTime().minus(this.armRpoDuration),
            this.objectRecordStatusService.getObjectRecordStatusEntity(ObjectRecordStatusEnum.STORED),
            Limit.of(this.getAutomatedTaskBatchSize())
        );
    }
}
