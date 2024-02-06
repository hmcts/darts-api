package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;

@ExtendWith(MockitoExtension.class)
class ExternalDataStoreDeleterAutomatedTaskTest {


    @Mock
    LockProvider provider;

    @Mock
    private ExternalInboundDataStoreDeleter inboundDeleter;

    @Mock
    private ExternalUnstructuredDataStoreDeleter unstructuredDeleter;

    @Mock
    private ExternalOutboundDataStoreDeleter outboundDeleter;

    @Test
    void runTask() {
        ExternalDataStoreDeleterAutomatedTask externalDataStoreDeleterAutomatedTask =
              new ExternalDataStoreDeleterAutomatedTask(null, provider, null, inboundDeleter, unstructuredDeleter, outboundDeleter);

        externalDataStoreDeleterAutomatedTask.runTask();

        Mockito.verify(inboundDeleter, Mockito.times(1)).delete();
        Mockito.verify(outboundDeleter, Mockito.times(1)).delete();
        Mockito.verify(unstructuredDeleter, Mockito.times(1)).delete();
    }
}
