package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.service.LockService;

@ExtendWith(MockitoExtension.class)
class ExternalDataStoreDeleterAutomatedTaskTest {

    @Mock
    private ExternalInboundDataStoreDeleter inboundDeleter;

    @Mock
    private ExternalUnstructuredDataStoreDeleter unstructuredDeleter;

    @Mock
    private ExternalOutboundDataStoreDeleter outboundDeleter;

    @Mock
    private LogApi logApi;

    @Mock
    private LockService lockService;

    @Test
    void runTask() {
        ExternalDataStoreDeleterAutomatedTask externalDataStoreDeleterAutomatedTask =
            new ExternalDataStoreDeleterAutomatedTask(
                null,null, inboundDeleter,
                unstructuredDeleter, outboundDeleter, logApi, lockService
            );

        externalDataStoreDeleterAutomatedTask.runTask();

        Mockito.verify(inboundDeleter, Mockito.times(1)).delete();
        Mockito.verify(outboundDeleter, Mockito.times(1)).delete();
        Mockito.verify(unstructuredDeleter, Mockito.times(1)).delete();
    }
}
