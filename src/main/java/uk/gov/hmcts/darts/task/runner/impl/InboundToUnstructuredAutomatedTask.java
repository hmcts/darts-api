package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.INBOUND_TO_UNSTRUCTURED_TASK_NAME;

@Slf4j
@SuppressWarnings({"squid:S1135"})
public class InboundToUnstructuredAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = INBOUND_TO_UNSTRUCTURED_TASK_NAME.getTaskName();
    private final InboundToUnstructuredProcessor inboundToUnstructuredProcessor;

    public InboundToUnstructuredAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                              LockProvider lockProvider,
                                              AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                              InboundToUnstructuredProcessor processor,
                                              LogApi logApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties, logApi);
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
