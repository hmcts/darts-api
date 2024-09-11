package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.INBOUND_TO_UNSTRUCTURED_TASK_NAME;

@Slf4j
@Component
@SuppressWarnings({"squid:S1135"})
public class InboundToUnstructuredAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    protected String taskName = INBOUND_TO_UNSTRUCTURED_TASK_NAME.getTaskName();
    private final InboundToUnstructuredProcessor inboundToUnstructuredProcessor;

    public InboundToUnstructuredAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                              AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                              InboundToUnstructuredProcessor processor,
                                              LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.inboundToUnstructuredProcessor = processor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        inboundToUnstructuredProcessor.processInboundToUnstructured();
    }

}
