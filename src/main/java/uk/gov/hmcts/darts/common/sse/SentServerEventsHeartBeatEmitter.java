package uk.gov.hmcts.darts.common.sse;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RequiredArgsConstructor
@Service
public class SentServerEventsHeartBeatEmitter {

    public static final String HEARTBEAT_EVENT_NAME = "heartbeat";
    private CountDownLatch latch = new CountDownLatch(1);

    @Setter
    @Getter
    @Value("${darts.sse.heartbeat: 5}")
    private long waitBetweenHeartBeats;

    private void heartBeat(SseEmitter emitter) throws InterruptedException {
        int counter = 0;
        emitter.onCompletion(latch::countDown);
        emitter.onError(e -> latch.countDown());
        try {
            do {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .id(String.valueOf(counter++))
                    .name(HEARTBEAT_EVENT_NAME);
                emitter.send(event);
                Thread.sleep(Duration.ofSeconds(waitBetweenHeartBeats).toMillis());
            } while (latch.getCount() > 0);
        } catch (IOException e) {
            DartsApiException dartsApiException = new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST);
            emitter.completeWithError(dartsApiException);
            log.error("Error while emitting heartbeat", e);
            throw dartsApiException;
        }
    }

    public void startHeartBeat(SseEmitter emitter) {
        latch = new CountDownLatch(1);
        ExecutorService emitterExecutor = Executors.newSingleThreadExecutor();
        emitterExecutor.execute(() -> {
            try {
                heartBeat(emitter);
            } catch (InterruptedException e) {
                log.error("Emitter interrupted while emitting heartbeat", e);
                emitter.completeWithError(e);
                Thread.currentThread().interrupt();
            }
        });
    }

}
