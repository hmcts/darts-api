package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.component.UnstructuredToArmProcessorFactory;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import java.util.Optional;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.UNSTRUCTURED_TO_ARM_TASK_NAME;

@Slf4j
public class UnstructuredToArmAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = UNSTRUCTURED_TO_ARM_TASK_NAME.getTaskName();
    private UnstructuredToArmProcessor unstructuredToArmProcessor;
    private final UnstructuredToArmProcessorFactory unstructuredToArmProcessorFactory;

    public UnstructuredToArmAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                          LockProvider lockProvider,
                                          AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                          UnstructuredToArmProcessorFactory processor,
                                          LogApi logApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties, logApi);
        this.unstructuredToArmProcessorFactory = processor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        Optional<AutomatedTaskEntity> automatedTaskEntity = getAutomatedTaskDetails(taskName);

        if (automatedTaskEntity.isEmpty()) {
            handleException(new Exception("Unable to find automated task details."));
        }

        AutomatedTaskEntity automatedTask = automatedTaskEntity.get();
        boolean isBatchMode = nonNull(automatedTask.getBatchSize()) && automatedTask.getBatchSize() > 0;

        log.info("Building UnstructuredToArmAutomatedTask processor");
        unstructuredToArmProcessor = unstructuredToArmProcessorFactory.createUnstructuredToArmProcessor(isBatchMode);
        unstructuredToArmProcessor.processUnstructuredToArm();
    }

    @SuppressWarnings("java:S4507")
    @Override
    protected void handleException(Exception exception) {
        log.error("{} Exception", getTaskName(), exception);
    }
}
