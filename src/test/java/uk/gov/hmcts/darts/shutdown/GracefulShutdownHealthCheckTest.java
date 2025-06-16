package uk.gov.hmcts.darts.shutdown;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GracefulShutdownHealthCheckTest {

    @Test
    void setReadyTrue_shouldSetHealthToUp() {
        GracefulShutdownHealthCheck healthCheck = new GracefulShutdownHealthCheck();
        healthCheck.setReady(true);
        Health health = healthCheck.health();
        assertEquals("UP", health.getStatus().getCode());
        assertEquals("application up", health.getDetails().get(GracefulShutdownHealthCheck.HEALTH_KEY));
    }

    @Test
    void setReadyFalse_shouldSetHealthToDown() {
        GracefulShutdownHealthCheck healthCheck = new GracefulShutdownHealthCheck();
        healthCheck.setReady(false);
        Health health = healthCheck.health();
        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals("gracefully shutting down", health.getDetails().get(GracefulShutdownHealthCheck.HEALTH_KEY));
    }
}
