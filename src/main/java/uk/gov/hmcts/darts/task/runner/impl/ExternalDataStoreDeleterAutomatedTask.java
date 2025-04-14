package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ExternalDataStoreDeleterAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.EXTERNAL_DATASTORE_DELETER_TASK_NAME;

@Slf4j
@Component
public class ExternalDataStoreDeleterAutomatedTask
    extends AbstractLockableAutomatedTask<ExternalDataStoreDeleterAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final ExternalInboundDataStoreDeleter inboundDeleter;
    private final ExternalUnstructuredDataStoreDeleter unstructuredDeleter;
    private final ExternalOutboundDataStoreDeleter outboundDeleter;


    @Autowired
    public ExternalDataStoreDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                 ExternalDataStoreDeleterAutomatedTaskConfig automatedTaskConfigurationProperties,
                                                 ExternalInboundDataStoreDeleter inboundDeleter,
                                                 ExternalUnstructuredDataStoreDeleter unstructuredDeleter,
                                                 ExternalOutboundDataStoreDeleter outboundDeleter,
                                                 LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.inboundDeleter = inboundDeleter;
        this.unstructuredDeleter = unstructuredDeleter;
        this.outboundDeleter = outboundDeleter;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return EXTERNAL_DATASTORE_DELETER_TASK_NAME;
    }

    @Override
    protected void runTask() {
        Integer batchSize = getAutomatedTaskBatchSize();
        log.info("Running External Data Store Deleter with batch size {}", batchSize);
        inboundDeleter.delete(batchSize);
        unstructuredDeleter.delete(batchSize);
        outboundDeleter.delete(batchSize);
    }
}
