package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.EXTERNAL_DATASTORE_DELETER_TASK_NAME;

@Slf4j
public class ExternalDataStoreDeleterAutomatedTask extends AbstractLockableAutomatedTask {

    private final ExternalInboundDataStoreDeleter inboundDeleter;
    private final ExternalUnstructuredDataStoreDeleter unstructuredDeleter;
    private final ExternalOutboundDataStoreDeleter outboundDeleter;


    protected String taskName = EXTERNAL_DATASTORE_DELETER_TASK_NAME.getTaskName();

    public ExternalDataStoreDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                 AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
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
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        inboundDeleter.delete();
        unstructuredDeleter.delete();
        outboundDeleter.delete();
    }
}
