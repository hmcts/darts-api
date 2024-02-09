package uk.gov.hmcts.darts.log.service.impl;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.log.enums.EventSource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class})
@SuppressWarnings("PMD.LawOfDemeter")
class EventLoggerServiceImplTest {

    EventLoggerServiceImpl eventLoggerService;

    private static LogCaptor logCaptor;

    @BeforeAll
    public static void setupLogCaptor() {
        logCaptor = LogCaptor.forClass(EventLoggerServiceImpl.class);
        logCaptor.setLogLevelToInfo();
    }

    @AfterEach
    public void clearLogs() {
        logCaptor.clearLogs();
    }

    @AfterAll
    public static void tearDown() {
        logCaptor.close();
    }

    @BeforeEach
    void setUp() {
        eventLoggerService = new EventLoggerServiceImpl("Xhibit Daily Test", "CPP Daily Test");
    }

    @Test
    void testLogsXhbPollCheck() {
        var event = createDartsEvent(123, "Xhibit Daily Test");
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, source=%s, poll_check=true, date_time=%s",
                                     event.getMessageId(), event.getEventId(), EventSource.XHB, event.getDateTime());
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    @Test
    void testLogsCppPollCheck() {
        var event = createDartsEvent(-123, "CPP Daily Test");
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, source=%s, poll_check=true, date_time=%s",
                                     event.getMessageId(), event.getEventId(), EventSource.CPP, event.getDateTime());
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    @Test
    void testLogsXhbEvent() {
        var event = createDartsEvent(123, "Some event text");
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, source=%s, date_time=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse(),
                                     event.getCourtroom(), EventSource.XHB, event.getDateTime());
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    @Test
    void testLogsCppEvent() {
        var event = createDartsEvent(-123, "Some event text");
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, source=%s, date_time=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse(),
                                     event.getCourtroom(), EventSource.CPP, event.getDateTime());
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    @Test
    void testHandlesEmptyEventText() {
        var event = createDartsEvent(-123, null);
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, source=%s, date_time=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse(),
                                     event.getCourtroom(), EventSource.CPP, event.getDateTime());
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    @Test
    void testHandlesNotIntegerEventType() {
        var event = createDartsEvent(0, "Some event text");
        event.setEventId("WRONG");
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, source=%s, date_time=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse(),
                                     event.getCourtroom(), EventSource.UNKNOWN, event.getDateTime());
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    @Test
    void testHandlesNullCourthouseCourtroom() {
        var event = createDartsEvent(0, "Some event text");
        event.setCourthouse(null);
        event.setCourtroom(null);
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, source=%s, date_time=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse(),
                                     event.getCourtroom(), EventSource.XHB, event.getDateTime());
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    private DartsEvent createDartsEvent(int eventId, String eventText) {
        var eventDate = OffsetDateTime.of(2024, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        DartsEvent event = new DartsEvent();
        event.setType("1000");
        event.setSubType("1002");
        event.setEventId(String.valueOf(eventId));
        event.setCourthouse("SWANSEA");
        event.setCourtroom("1");
        event.setMessageId("test-message-id");
        event.setEventText(eventText);
        event.setDateTime(eventDate);
        return event;
    }

}
