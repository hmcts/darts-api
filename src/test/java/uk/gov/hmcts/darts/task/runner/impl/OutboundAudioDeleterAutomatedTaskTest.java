package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;

@ExtendWith(MockitoExtension.class)
class OutboundAudioDeleterAutomatedTaskTest {

    @Mock
    OutboundAudioDeleterProcessor processor;

    @Mock
    LockProvider provider;

    @Test
    void runTask() {
        OutboundAudioDeleterAutomatedTask outboundAudioDeleterAutomatedTask =
              new OutboundAudioDeleterAutomatedTask(null, provider, null, processor);

        outboundAudioDeleterAutomatedTask.runTask();

        Mockito.verify(processor, Mockito.times(1)).markForDeletion();
    }
}
