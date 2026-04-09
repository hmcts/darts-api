package uk.gov.hmcts.darts.shutdown;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("DartsGracefulShutdownHealthCheck")
@Slf4j
public class GracefulShutdownHealthCheck {
    public static final String HEALTH_KEY = "DartsGracefulShutdown";

    @Getter
    private boolean ready = true;

    public void setReady(boolean ready) {
        log.info("Updating application graceful shutdown health check state to: {}", ready ? "up" : "down");
        this.ready = ready;
    }
}
