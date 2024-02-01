package uk.gov.hmcts.darts.common.sse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@AllArgsConstructor
public class SentServerEventsHeartBeatEmitter {

    public static final String HEARTBEAT_EVENT_NAME = "heartbeat";
    private final CountDownLatch latch = new CountDownLatch(1);
    private Duration waitBetweenHeartBeats;

    private void heartBeat(SseEmitter emitter) {
        int counter = 0;
        emitter.onCompletion(latch::countDown);
        try {
            do {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .id(String.valueOf(counter++))
                    .name(HEARTBEAT_EVENT_NAME);
                emitter.send(event);
                Thread.sleep(waitBetweenHeartBeats.toMillis());
            } while (latch.getCount() > 0);
        } catch (Exception e) {
            DartsApiException dartsApiException = new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST);
            emitter.completeWithError(dartsApiException);
            log.error("Error while emitting heartbeat", e);
            throw dartsApiException;
        }
    }

    public void startHeartBeat(SseEmitter emitter) {
        ExecutorService emitterExecutor = Executors.newSingleThreadExecutor();
        emitterExecutor.execute(() -> heartBeat(emitter));
    }
}
