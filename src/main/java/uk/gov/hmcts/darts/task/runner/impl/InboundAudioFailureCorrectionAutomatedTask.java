package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.service.InboundAudioFailureCorrectionService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.InboundAudioFailureCorrectionAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

@Slf4j
@Component
public class InboundAudioFailureCorrectionAutomatedTask extends AbstractLockableAutomatedTask<InboundAudioFailureCorrectionAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final InboundAudioFailureCorrectionService inboundAudioFailureCorrectionService;

    @Autowired
    public InboundAudioFailureCorrectionAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                      InboundAudioFailureCorrectionAutomatedTaskConfig inboundAudioFailureCorrectionAutomatedTaskConfig,
                                                      LogApi logApi, LockService lockService,
                                                      InboundAudioFailureCorrectionService inboundAudioFailureCorrectionService) {
        super(automatedTaskRepository, inboundAudioFailureCorrectionAutomatedTaskConfig, logApi, lockService);
        this.inboundAudioFailureCorrectionService = inboundAudioFailureCorrectionService;
    }

    @Override
    protected void runTask() {
        inboundAudioFailureCorrectionService.correctAudioFailure(getAutomatedTaskBatchSize());
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.INBOUND_AUDIO_FAILURE_CORRECTION_TASK_NAME;
    }
}
