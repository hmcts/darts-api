package uk.gov.hmcts.darts.shutdown;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;


import java.time.Duration;

@RequiredArgsConstructor
@Slf4j
//TODO Fix loging so we still get logs even after logging shutdown hook is triggered
public class GracefulShutdownHook
    implements Runnable, GracefulShutdownCallback {
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run() {
        setReadinessToFalse();
        delayShutdown();
        shutdownApplication();
    }

    void setReadinessToFalse() {
        log.info("Setting readiness for application to false, so the application doesn't receive new connections from now on.");
        GracefulShutdownHealthCheck probeControllers = applicationContext.getBean(
            "DartsGracefulShutdownHealthCheck", GracefulShutdownHealthCheck.class);
        probeControllers.setReady(false);
    }

    //Required for graceful shutdown. Health check fails for a short time, so the load balancer stops sending new requests.
    @SuppressWarnings("PMD.DoNotUseThreads")
    void delayShutdown() {
        try {
            String waitTimeString = applicationContext.getBeanFactory().resolveEmbeddedValue("${darts.shutdown.wait-time:30s}");
            Duration waitTime = DurationStyle.detectAndParse(waitTimeString);
            log.info("Gonna wait for " + waitTime + " before shutdown SpringContext!");
            Thread.sleep(waitTime.toMillis());
        } catch (InterruptedException e) {
            log.error("Error while gracefulshutdown Thread.sleep", e);
            Thread.currentThread().interrupt();
        }
    }

    void shutdownApplication() {
        log.info("Shutting down Application");
        // First try graceful shutdown of the web server (if present), then close the context.
        try {
            var webServer = applicationContext.getBean("webServer", Object.class);
            // If the web server bean isn't exposed under this name, fall back to just closing the context.
            log.debug("Web server bean found ({}), attempting graceful shutdown", webServer.getClass().getName());
        } catch (Exception ignored) {
            // ignore
        }
        applicationContext.close();
    }

    @Override
    public void shutdownComplete(GracefulShutdownResult result) {
        log.info("Graceful shutdown complete: {}", result);
    }
}
