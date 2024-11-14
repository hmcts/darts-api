package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.InboundToUnstructuredAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.INBOUND_TO_UNSTRUCTURED_TASK_NAME;

@Slf4j
@Component
public class InboundToUnstructuredAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final InboundToUnstructuredProcessor inboundToUnstructuredProcessor;

    @Autowired
    public InboundToUnstructuredAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                              InboundToUnstructuredAutomatedTaskConfig automatedTaskConfigurationProperties,
                                              InboundToUnstructuredProcessor processor,
                                              LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.inboundToUnstructuredProcessor = processor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return INBOUND_TO_UNSTRUCTURED_TASK_NAME;
    }

    @Override
    protected void runTask() {
        inboundToUnstructuredProcessor.processInboundToUnstructured(getAutomatedTaskBatchSize());
    }
}
