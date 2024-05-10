package uk.gov.hmcts.darts.log.service.impl;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.log.enums.EventSource;
import uk.gov.hmcts.darts.log.service.EventLoggerService;

import java.time.OffsetDateTime;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class EventLoggerServiceImpl implements EventLoggerService {

    @Value("${darts.log.events.daily-test-event-text.xhb}")
    private String xhbDailyTestEventText;

    @Value("${darts.log.events.daily-test-event-text.cpp}")
    private String cppDailyTestEventText;

    @Override
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void eventReceived(DartsEvent event) {
        var isPollCheck = false;
        EventSource source = EventSource.UNKNOWN;
        try {
            if (event.getIsMidTier()) {
                source = EventSource.MIDTIER;
            } else if (Integer.parseInt(event.getEventId()) >= 0) {
                // XHIBIT sends event positive IDs, CPP send negative event IDs
                source = EventSource.XHB;
                if (StringUtils.equals(event.getEventText(), xhbDailyTestEventText)) {
                    isPollCheck = true;
                }
            } else {
                source = EventSource.CPP;
                if (StringUtils.equals(event.getEventText(), cppDailyTestEventText)) {
                    isPollCheck = true;
                }
            }
        } catch (NumberFormatException e) {
            // continue with source UNKNOWN
        }
        if (isPollCheck) {
            logPollCheck(event.getMessageId(), event.getEventId(), source, event.getDateTime());
        } else {
            logEvent(event.getMessageId(), event.getEventId(), event.getCourthouse(),
                     event.getCourtroom(), source, event.getDateTime());
        }
    }

    private void logPollCheck(String messageId, String eventId, EventSource source, OffsetDateTime dateTime) {
        log.info("Event received: message_id={}, event_id={}, source={}, poll_check=true, date_time={}",
                 messageId, eventId, source, dateTime);
    }

    private void logEvent(String messageId, String eventId, String courthouse, String courtroom, EventSource source, OffsetDateTime dateTime) {
        log.info("Event received: message_id={}, event_id={}, courthouse={}, courtroom={}, source={}, date_time={}",
                 messageId, eventId, courthouse, courtroom, source, dateTime);
    }

    @Override
    public void missingCourthouse(DartsEvent event) {
        log.error("Courthouse not found: message_id={}, event_id={}, courthouse={}, courtroom={}, event_timestamp={}",
                  event.getMessageId(),
                  event.getEventId(),
                  event.getCourthouse(),
                  event.getCourtroom(),
                  event.getDateTime());
    }

    @Override
    public void missingNodeRegistry(DartsEvent event) {
        log.error("Unregistered Room: message_id={}, event_id={}, courthouse={}, courtroom={}, event_timestamp={}",
                  event.getMessageId(),
                  event.getEventId(),
                  event.getCourthouse(),
                  event.getCourtroom(),
                  event.getDateTime());
    }
}
