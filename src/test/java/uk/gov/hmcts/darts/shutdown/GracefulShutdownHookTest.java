package uk.gov.hmcts.darts.shutdown;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GracefulShutdownHookTest {

    private GracefulShutdownHook gracefulShutdownHook;
    private ServletWebServerApplicationContext applicationContext;

    @BeforeEach
    void beforeEach() {
        applicationContext = mock(ServletWebServerApplicationContext.class);
        gracefulShutdownHook = spy(new GracefulShutdownHook(applicationContext, mock(TelemetryClient.class)));
    }

    @Test
    void run_shouldSetReadinessToFalseAndDelayShutDownAndShutdownApplication() {
        doNothing().when(gracefulShutdownHook).setReadinessToFalse();
        doNothing().when(gracefulShutdownHook).delayShutdown();
        doNothing().when(gracefulShutdownHook).shutdownApplication();

        gracefulShutdownHook.run();
        verify(gracefulShutdownHook).setReadinessToFalse();
        verify(gracefulShutdownHook).delayShutdown();
        verify(gracefulShutdownHook).shutdownApplication();
    }

    @Test
    void setReadinessToFalse_shouldSetHealthCheckBeanReadyToFalse() {
        GracefulShutdownHealthCheck probeControllers = mock(GracefulShutdownHealthCheck.class);
        when(applicationContext.getBean("DartsGracefulShutdownHealthCheck", GracefulShutdownHealthCheck.class))
            .thenReturn(probeControllers);
        gracefulShutdownHook.setReadinessToFalse();
        verify(probeControllers).setReady(false);
    }

    @Test
    void delayShutdown_shouldSleepForConfiguredTime() throws InterruptedException {
        String waitTimeString = "2s";
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        when(applicationContext.getBeanFactory()).thenReturn(beanFactory);

        when(beanFactory.resolveEmbeddedValue(any())).thenReturn(waitTimeString);

        Long waitStartTime = System.currentTimeMillis();

        gracefulShutdownHook.delayShutdown();
        Long waitEndTime = System.currentTimeMillis();
        Long waitDuration = waitEndTime - waitStartTime;

        assertTrue(waitDuration >= 2000,
                   "Wait duration was less than 2000ms actual: " + waitDuration + "ms");

        verify(applicationContext).getBeanFactory();
        verify(beanFactory).resolveEmbeddedValue("${darts.shutdown.wait-time:30s}");
    }

    @Test
    void shutdownComplete_shouldCloseApplicationContext() {
        WebServer webServer = mock(WebServer.class);
        when(applicationContext.getWebServer()).thenReturn(webServer);

        gracefulShutdownHook.shutdownApplication();

        verify(applicationContext).getWebServer();
        verify(webServer).shutDownGracefully(gracefulShutdownHook);
    }

    @Test
    void shutdownComplete_shouldCloseApplicationContextAndLogResult() {
        GracefulShutdownResult result = mock(GracefulShutdownResult.class);
        gracefulShutdownHook.shutdownComplete(result);

        verify(applicationContext).close();
    }
}
