package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.arm.service.impl.DetsToArmBatchProcessResponseFilesImpl;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.PROCESS_DETS_TO_ARM_RESPONSE;

@Slf4j
@Component
public class ProcessDetsToArmResponseAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    @Autowired
    public ProcessDetsToArmResponseAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                 AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                 AutomatedTaskProcessorFactory automatedTaskProcessorFactory,
                                                 LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.automatedTaskProcessorFactory = automatedTaskProcessorFactory;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return PROCESS_DETS_TO_ARM_RESPONSE;
    }

    @Override
    protected void runTask() {
        Integer batchSize = getAutomatedTaskBatchSize();
        DetsToArmBatchProcessResponseFilesImpl detsToArmResponseFilesProcessor = automatedTaskProcessorFactory.createDetsToArmResponseFilesProcessor(batchSize);
        detsToArmResponseFilesProcessor.processResponseFiles();
    }
}