package uk.gov.hmcts.darts.common.sse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class SentServerEventsHeartBeatEmitterTest {

    @Mock
    SseEmitter emitter;
    @Captor
    ArgumentCaptor<Throwable> exceptionCaptor;

    @Test
    void shouldCreateHeartBeat() throws IOException, InterruptedException {
        SentServerEventsHeartBeatEmitter heartbeatEmitter = new SentServerEventsHeartBeatEmitter(Duration.ofSeconds(2));
        heartbeatEmitter.startHeartBeat(emitter);
        Thread.sleep(5000);
        Mockito.verify(emitter, Mockito.times(3)).send(any(SseEmitter.SseEventBuilder.class));

    }

    @Test
    void shouldThrowError() throws IOException, InterruptedException {
        Mockito.doThrow(new IOException()).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        SentServerEventsHeartBeatEmitter heartbeatEmitter = new SentServerEventsHeartBeatEmitter(Duration.ofSeconds(1));
        heartbeatEmitter.startHeartBeat(emitter);
        Thread.sleep(5000);
        Mockito.verify(emitter).completeWithError(exceptionCaptor.capture());
        assertEquals("Failed to process audio request", exceptionCaptor.getValue().getMessage());
    }
}
