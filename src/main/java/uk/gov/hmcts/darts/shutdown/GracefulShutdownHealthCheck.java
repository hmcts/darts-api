package uk.gov.hmcts.darts.shutdown;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("DartsGracefulShutdownHealthCheck")
@Slf4j
public class GracefulShutdownHealthCheck implements HealthIndicator {
    public static final String HEALTH_KEY = "DartsGracefulShutdown";

    @Getter
    private Health healthResult;

    @Override
    public Health health() {
        return healthResult;
    }

    public void setReady(boolean ready) {
        log.info("Updating application graceful shutdown health check state to: {}", ready ? "up" : "down");
        if (ready) {
            healthResult = new Health.Builder().withDetail(HEALTH_KEY, "application up").up().build();
        } else {
            healthResult = new Health.Builder().withDetail(HEALTH_KEY, "gracefully shutting down").down().build();
        }
    }
}
