package uk.gov.hmcts.darts.log.service.impl;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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
        eventLoggerService = new EventLoggerServiceImpl();
        ReflectionTestUtils.setField(eventLoggerService, "xhbDailyTestEventText", "Xhibit Daily Test");
        ReflectionTestUtils.setField(eventLoggerService, "cppDailyTestEventText", "CPP Daily Test");
        ReflectionTestUtils.setField(eventLoggerService, "xhbEventTypeLength", 5);
        ReflectionTestUtils.setField(eventLoggerService, "cppEventTypeLength", 4);
    }

    @Test
    void testLogsXhbPollCheck() {
        var event = createDartsEvent("20705", "10703", "Xhibit Daily Test");
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, source=%s, poll_check=true, date_time=%s",
                                     event.getMessageId(), event.getEventId(), EventSource.XHB, event.getDateTime());
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    @Test
    void testLogsCppPollCheck() {
        var event = createDartsEvent("20705", "10703", "CPP Daily Test");
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, source=%s, poll_check=true, date_time=%s",
                                     event.getMessageId(), event.getEventId(), EventSource.CPP, event.getDateTime());
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    @Test
    void testLogsXhbEvent() {
        var event = createDartsEvent("10200", "1002", "Some event text");
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
        var event = createDartsEvent("1000", "1002", "Some event text");
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
        var event = createDartsEvent("1000", "1002", null);
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, source=%s, date_time=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse(),
                                     event.getCourtroom(), EventSource.CPP, event.getDateTime());
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    @Test
    void testHandlesEmptyEventType() {
        var event = createDartsEvent(null, "1002", "Some event text");
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
        var event = createDartsEvent("1000", "1002", "Some event text");
        event.setCourthouse(null);
        event.setCourtroom(null);
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, source=%s, date_time=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse(),
                                     event.getCourtroom(), EventSource.CPP, event.getDateTime());
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    private DartsEvent createDartsEvent(String eventType, String eventSubType, String eventText) {
        var eventDate = OffsetDateTime.of(2024, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        DartsEvent event = new DartsEvent();
        event.setType(eventType);
        event.setSubType(eventSubType);
        event.setEventId("4354");
        event.setCourthouse("SWANSEA");
        event.setCourtroom("1");
        event.setMessageId("test-message-id");
        event.setEventText(eventText);
        event.setDateTime(eventDate);
        return event;
    }

}
