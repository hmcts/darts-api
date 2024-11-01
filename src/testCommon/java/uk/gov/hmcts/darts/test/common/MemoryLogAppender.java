package uk.gov.hmcts.darts.test.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MemoryLogAppender extends AppenderBase<ILoggingEvent> {

    public static final List<ILoggingEvent> GENERAL_LOGS = Collections.synchronizedList(new ArrayList<>());

    public static final String LOG_API_LOGGER_NAME_PACKAGE_PREFIX = "uk.gov.hmcts.darts.log";

    public void reset() {
        GENERAL_LOGS.clear();
    }

    public List<ILoggingEvent> searchLogApiLogs(String string, Level level) {
        return this.GENERAL_LOGS.stream()
            .filter(event -> event.getLoggerName().startsWith(LOG_API_LOGGER_NAME_PACKAGE_PREFIX)
                && event.toString().contains(string)
                && event.getLevel().equals(level))
            .collect(Collectors.toList());
    }

    public List<ILoggingEvent> searchLogs(String string, Level level) {
        return this.GENERAL_LOGS.stream()
            .filter(event -> event.toString().contains(string)
                && event.getLevel().equals(level))
            .collect(Collectors.toList());
    }

    public List<ILoggingEvent> searchLogs(String string, String causeMessage, Level level) {
        return GENERAL_LOGS.stream()
            .filter(event -> event.toString().contains(string)
                && event.getThrowableProxy().getMessage().contains(causeMessage)
                && event.getLevel().equals(level))
            .collect(Collectors.toList());
    }

    public boolean hasLogApiCallTakenPlace() {
        return !GENERAL_LOGS.stream()
            .filter(event -> event.getLoggerName().startsWith(LOG_API_LOGGER_NAME_PACKAGE_PREFIX)).collect(Collectors.toList()).isEmpty();
    }

    @Override
    protected void append(ILoggingEvent event) {
        GENERAL_LOGS.add(event);
    }
}