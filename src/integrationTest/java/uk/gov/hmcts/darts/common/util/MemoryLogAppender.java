package uk.gov.hmcts.darts.common.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MemoryAppender extends AppenderBase<ILoggingEvent> {

    public static final List<ILoggingEvent> LOG_API_LOG_LIST = new ArrayList<>();
    public static final List<ILoggingEvent> GENERAL_LOGS = new ArrayList<>();

    public void reset() {
        LOG_API_LOG_LIST.clear();
        GENERAL_LOGS.clear();
    }

    public List<ILoggingEvent> searchLogApiLogs(String string, Level level) {
        return this.LOG_API_LOG_LIST.stream()
            .filter(event -> event.toString().contains(string)
                && event.getLevel().equals(level))
            .collect(Collectors.toList());
    }

    public List<ILoggingEvent> searchLogs(String string, Level level) {
        return this.GENERAL_LOGS.stream()
            .filter(event -> event.toString().contains(string)
                && event.getLevel().equals(level))
            .collect(Collectors.toList());
    }

    public boolean hasLogApiCallTakenPlace() {
        return !LOG_API_LOG_LIST.isEmpty();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (event.getLoggerName().startsWith("uk.gov.hmcts.darts.log")) {
            LOG_API_LOG_LIST.add(event);
        }

        GENERAL_LOGS.add(event);
    }
}