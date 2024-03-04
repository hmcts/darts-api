package uk.gov.hmcts.darts.common.util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

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
}