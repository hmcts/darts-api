package uk.gov.hmcts.darts.shutdown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GracefulShutdownHealthCheckTest {

    @Test
    void setReadyTrue_shouldSetHealthToUp() {
        GracefulShutdownHealthCheck healthCheck = new GracefulShutdownHealthCheck();
        healthCheck.setReady(true);
        assertEquals(true, healthCheck.isReady());
    }

    @Test
    void setReadyFalse_shouldSetHealthToDown() {
        GracefulShutdownHealthCheck healthCheck = new GracefulShutdownHealthCheck();
        healthCheck.setReady(false);
        assertEquals(false, healthCheck.isReady());
    }
}
