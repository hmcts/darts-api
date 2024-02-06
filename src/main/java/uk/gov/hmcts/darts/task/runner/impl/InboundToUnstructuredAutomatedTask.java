package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.INBOUND_TO_UNSTRUCTURED_TASK_NAME;

@Slf4j
@SuppressWarnings({"squid:S1135"})
public class InboundToUnstructuredAutomatedTask extends AbstractLockableAutomatedTask {

    private final InboundToUnstructuredProcessor inboundToUnstructuredProcessor;
    protected String taskName = INBOUND_TO_UNSTRUCTURED_TASK_NAME.getTaskName();

    public InboundToUnstructuredAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
          LockProvider lockProvider,
          AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
          InboundToUnstructuredProcessor processor) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
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

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
    }

}
