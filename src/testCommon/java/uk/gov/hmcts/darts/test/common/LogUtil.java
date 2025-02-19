package uk.gov.hmcts.darts.test.common;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import lombok.SneakyThrows;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.fail;

public final class LogUtil {

    private LogUtil() {

    }

    public static MemoryLogAppender getMemoryLogger() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        for (Logger logger : loggerContext.getLoggerList()) {
            Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();

            while (appenderIterator.hasNext()) {
                Appender appender = appenderIterator.next();
                if (appender instanceof MemoryLogAppender) {
                    return (MemoryLogAppender) appender;
                }
            }
        }
        return null;
    }

    @SneakyThrows
    @SuppressWarnings("PMD.DoNotUseThreads")//Required to prevent busy waiting
    //Used to allow logs to catch up with the test
    public static void waitUntilMessage(CapturedOutput capturedOutput, String message,
                                        int timeoutInSeconds) {
        long startTime = System.currentTimeMillis();
        while (!capturedOutput.getAll().contains(message)) {
            if (System.currentTimeMillis() - startTime > timeoutInSeconds * 1000) {
                fail("Timeout waiting for message: " + message);
            }
            Thread.sleep(100);
        }
    }
}