package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.service.LockService;

@ExtendWith(MockitoExtension.class)
class OutboundAudioDeleterAutomatedTaskTest {

    @Mock
    OutboundAudioDeleterProcessor processor;

    @Mock
    private LogApi logApi;

    @Mock
    private LockService lockService;

    @Test
    void runTask() {
        OutboundAudioDeleterAutomatedTask outboundAudioDeleterAutomatedTask =
            new OutboundAudioDeleterAutomatedTask(null, null, processor, logApi, lockService);

        outboundAudioDeleterAutomatedTask.runTask();

        Mockito.verify(processor, Mockito.times(1)).markForDeletion();
    }
}
