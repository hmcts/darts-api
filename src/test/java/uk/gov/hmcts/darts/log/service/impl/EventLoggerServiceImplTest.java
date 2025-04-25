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
import java.util.Locale;

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
        logCaptor.clearLogs();
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
        var event = createDartsEvent(123, "Xhibit Daily Test", false);
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, source=%s, poll_check=true, date_time=%s",
                                     event.getMessageId(), event.getEventId(), EventSource.XHB, "2024-10-10T10:00:00Z");
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.getFirst());
    }

    @Test
    void testLogsCppPollCheck() {
        var event = createDartsEvent(-123, "CPP Daily Test", false);
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, source=%s, poll_check=true, date_time=%s",
                                     event.getMessageId(), event.getEventId(), EventSource.CPP, "2024-10-10T10:00:00Z");
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.getFirst());
    }

    @Test
    void testLogsMidTierEvent() {
        var event = createDartsEvent(123, "Some event text", true);
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, source=%s, date_time=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse().toUpperCase(Locale.ROOT),
                                     event.getCourtroom().toUpperCase(Locale.ROOT), EventSource.MIDTIER, "2024-10-10T10:00:00Z");
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.getFirst());
    }

    @Test
    void testLogsXhbEvent() {
        var event = createDartsEvent(123, "Some event text", false);
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, source=%s, date_time=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse().toUpperCase(Locale.ROOT),
                                     event.getCourtroom().toUpperCase(Locale.ROOT), EventSource.XHB, "2024-10-10T10:00:00Z");
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.getFirst());
    }

    @Test
    void testLogsCppEvent() {
        var event = createDartsEvent(-123, "Some event text", false);
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, source=%s, date_time=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse().toUpperCase(Locale.ROOT),
                                     event.getCourtroom().toUpperCase(Locale.ROOT), EventSource.CPP, "2024-10-10T10:00:00Z");
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.getFirst());
    }

    @Test
    void testHandlesEmptyEventText() {
        var event = createDartsEvent(-123, null, false);
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, source=%s, date_time=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse().toUpperCase(Locale.ROOT),
                                     event.getCourtroom().toUpperCase(Locale.ROOT), EventSource.CPP, "2024-10-10T10:00:00Z");
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.getFirst());
    }

    @Test
    void testHandlesNotIntegerEventType() {
        var event = createDartsEvent(0, "Some event text", false);
        event.setEventId("WRONG");
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, source=%s, date_time=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse().toUpperCase(Locale.ROOT),
                                     event.getCourtroom().toUpperCase(Locale.ROOT), EventSource.UNKNOWN, "2024-10-10T10:00:00Z");
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.getFirst());
    }

    @Test
    void testHandlesNullCourthouseCourtroom() {
        var event = createDartsEvent(0, "Some event text", false);
        event.setCourthouse(null);
        event.setCourtroom(null);
        eventLoggerService.eventReceived(event);
        var logEntry = String.format("Event received: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, source=%s, date_time=%s",
                                     event.getMessageId(), event.getEventId(), null, null, EventSource.XHB, "2024-10-10T10:00:00Z");
        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.getFirst());
    }

    @Test
    void testLogsMissingCourthouse() {
        var event = createDartsEvent(123, "Some event text", false);
        event.setCourthouse("NOT_EXIST");
        eventLoggerService.missingCourthouse(event);
        var logEntry = String.format("Courthouse not found: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, timestamp=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse().toUpperCase(Locale.ROOT),
                                     event.getCourtroom().toUpperCase(Locale.ROOT), "2024-10-10T10:00:00Z");
        List<String> errorLogs = logCaptor.getErrorLogs();
        assertEquals(1, errorLogs.size());
        assertEquals(logEntry, errorLogs.getFirst());
    }

    @Test
    void testLogsMissingNodeRegistry() {
        var event = createDartsEvent(123, "Some event text", false);
        eventLoggerService.missingNodeRegistry(event);
        var logEntry = String.format("Unregistered Room: message_id=%s, event_id=%s, courthouse=%s, courtroom=%s, event_timestamp=%s",
                                     event.getMessageId(), event.getEventId(), event.getCourthouse().toUpperCase(Locale.ROOT),
                                     event.getCourtroom().toUpperCase(Locale.ROOT), "2024-10-10T10:00:00Z");
        List<String> errorLogs = logCaptor.getErrorLogs();
        assertEquals(1, errorLogs.size());
        assertEquals(logEntry, errorLogs.getFirst());
    }

    private DartsEvent createDartsEvent(int eventId, String eventText, boolean isMidTier) {
        var eventDate = OffsetDateTime.of(2024, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        DartsEvent event = new DartsEvent();
        event.setType("1000");
        event.setSubType("1002");
        event.setEventId(String.valueOf(eventId));
        event.setCourthouse("SwAnSeA");
        event.setCourtroom("RooM1");
        event.setMessageId("test-message-id");
        event.setEventText(eventText);
        event.setDateTime(eventDate);
        event.setIsMidTier(isMidTier);
        return event;
    }

}
