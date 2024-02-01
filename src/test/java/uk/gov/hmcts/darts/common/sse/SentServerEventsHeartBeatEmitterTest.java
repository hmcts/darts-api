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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class SentServerEventsHeartBeatEmitterTest {

    @Mock
    SseEmitter emitter;
    @Captor
    ArgumentCaptor<Throwable> exceptionCaptor;

    @Test
    void shouldCreateHeartBeat() throws IOException {
        SentServerEventsHeartBeatEmitter heartbeatEmitter = new SentServerEventsHeartBeatEmitter(Duration.ofSeconds(2));
        heartbeatEmitter.startHeartBeat(emitter);
        Mockito.verify(emitter, Mockito.timeout(5000).times(1)).send(any(SseEmitter.SseEventBuilder.class));

    }

    @Test
    void shouldThrowError() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(invocationOnMock -> {
            Object result = invocationOnMock.callRealMethod();
            latch.countDown();
            return result;
        }).when(emitter).completeWithError(exceptionCaptor.capture());

        Mockito.doThrow(new IOException()).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        SentServerEventsHeartBeatEmitter heartbeatEmitter = new SentServerEventsHeartBeatEmitter(Duration.ofSeconds(1));
        heartbeatEmitter.startHeartBeat(emitter);

        boolean result = latch.await(2, TimeUnit.SECONDS);
        if (result) {
            assertEquals("Failed to process audio request", exceptionCaptor.getValue().getMessage());
        } else {
            fail("Emitter did not complete with errors");
        }
    }
}
