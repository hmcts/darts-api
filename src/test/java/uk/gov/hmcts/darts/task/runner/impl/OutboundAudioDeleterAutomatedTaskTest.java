package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;

@ExtendWith(MockitoExtension.class)
class OutboundAudioDeleterAutomatedTaskTest {

    @Mock
    OutboundAudioDeleterProcessor processor;

    @Mock
    LockProvider provider;

    @Mock
    private LogApi logApi;

    @Test
    void runTask() {
        OutboundAudioDeleterAutomatedTask outboundAudioDeleterAutomatedTask =
            new OutboundAudioDeleterAutomatedTask(null, provider, null, processor, logApi);

        outboundAudioDeleterAutomatedTask.runTask();

        Mockito.verify(processor, Mockito.times(1)).markForDeletion();
    }
}
