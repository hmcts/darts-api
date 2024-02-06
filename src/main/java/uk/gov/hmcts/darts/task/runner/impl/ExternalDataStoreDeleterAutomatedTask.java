package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.EXTERNAL_DATASTORE_DELETER;

@Slf4j
public class ExternalDataStoreDeleterAutomatedTask extends AbstractLockableAutomatedTask {

    private final ExternalInboundDataStoreDeleter inboundDeleter;
    private final ExternalUnstructuredDataStoreDeleter unstructuredDeleter;
    private final ExternalOutboundDataStoreDeleter outboundDeleter;


    protected String taskName = EXTERNAL_DATASTORE_DELETER.getTaskName();

    public ExternalDataStoreDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository, LockProvider lockProvider,
                                                 AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                 ExternalInboundDataStoreDeleter inboundDeleter,
                                                 ExternalUnstructuredDataStoreDeleter unstructuredDeleter,
                                                 ExternalOutboundDataStoreDeleter outboundDeleter) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
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

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
    }
}
