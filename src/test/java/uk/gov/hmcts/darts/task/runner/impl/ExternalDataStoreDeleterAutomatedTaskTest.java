package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalOutboundDataStoreDeleterWithBuffer;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.service.LockService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class ExternalDataStoreDeleterAutomatedTaskTest {

    @Mock
    private ExternalInboundDataStoreDeleter inboundDeleter;

    @Mock
    private ExternalUnstructuredDataStoreDeleter unstructuredDeleter;

    @Mock
    private ExternalOutboundDataStoreDeleterWithBuffer outboundDeleter;

    private ExternalDataStoreDeleterAutomatedTask externalDataStoreDeleterAutomatedTask;

    @Mock
    private LogApi logApi;

    @Mock
    private LockService lockService;

    @BeforeEach
    void beforeEach() {
        externalDataStoreDeleterAutomatedTask =
            spy(new ExternalDataStoreDeleterAutomatedTask(
                null, null, inboundDeleter,
                unstructuredDeleter, outboundDeleter, logApi, lockService
            ));
        doReturn(100).when(externalDataStoreDeleterAutomatedTask).getAutomatedTaskBatchSize();

    }

    @Test
    void runTask_ShouldSucceed() {

        externalDataStoreDeleterAutomatedTask.runTask();

        Mockito.verify(inboundDeleter, Mockito.times(1)).delete(100);
        Mockito.verify(outboundDeleter, Mockito.times(1)).delete(100);
        Mockito.verify(unstructuredDeleter, Mockito.times(1)).delete(100);
        Mockito.verify(externalDataStoreDeleterAutomatedTask, Mockito.times(1)).getAutomatedTaskBatchSize();
    }

    @Test
    void runTask_ShouldThrowInterruptedException_WhenInterruptExceptionThrownByInboundDeleter() {

        doAnswer(invocation -> {
            throw new InterruptedException("Simulated interruption");
        }).when(inboundDeleter).delete(100);

        assertThrows(InterruptedException.class, () ->
            externalDataStoreDeleterAutomatedTask.runTask());

        Mockito.verify(inboundDeleter, Mockito.times(1)).delete(100);
        Mockito.verify(outboundDeleter, Mockito.times(0)).delete(100);
        Mockito.verify(unstructuredDeleter, Mockito.times(0)).delete(100);
        Mockito.verify(externalDataStoreDeleterAutomatedTask, Mockito.times(1)).getAutomatedTaskBatchSize();

    }

    @Test
    void runTask_ShouldThrowInterruptedException_WhenInterruptExceptionThrownByOutboundDeleter() {

        doAnswer(invocation -> {
            throw new InterruptedException("Simulated interruption");
        }).when(outboundDeleter).delete(100);

        assertThrows(InterruptedException.class, () ->
            externalDataStoreDeleterAutomatedTask.runTask());

        Mockito.verify(inboundDeleter, Mockito.times(1)).delete(100);
        Mockito.verify(outboundDeleter, Mockito.times(1)).delete(100);
        Mockito.verify(unstructuredDeleter, Mockito.times(1)).delete(100);
        Mockito.verify(externalDataStoreDeleterAutomatedTask, Mockito.times(1)).getAutomatedTaskBatchSize();

    }

    @Test
    void runTask_ShouldThrowInterruptedException_WhenInterruptExceptionThrownByUnstructuredDeleter() {
        
        doAnswer(invocation -> {
            throw new InterruptedException("Simulated interruption");
        }).when(unstructuredDeleter).delete(100);

        assertThrows(InterruptedException.class, () ->
            externalDataStoreDeleterAutomatedTask.runTask());

        Mockito.verify(inboundDeleter, Mockito.times(1)).delete(100);
        Mockito.verify(outboundDeleter, Mockito.times(0)).delete(100);
        Mockito.verify(unstructuredDeleter, Mockito.times(1)).delete(100);
        Mockito.verify(externalDataStoreDeleterAutomatedTask, Mockito.times(1)).getAutomatedTaskBatchSize();

    }
}
