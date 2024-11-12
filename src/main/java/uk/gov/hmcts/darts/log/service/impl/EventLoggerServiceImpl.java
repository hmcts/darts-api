package uk.gov.hmcts.darts.log.service.impl;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.log.enums.EventSource;
import uk.gov.hmcts.darts.log.service.EventLoggerService;
import uk.gov.hmcts.darts.util.DataUtil;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.util.DateTimeHelper.getDateTimeIsoFormatted;

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
                 messageId, eventId, source, getDateTimeIsoFormatted(dateTime));
    }

    private void logEvent(String messageId, String eventId, String courthouse, String courtroom, EventSource source, OffsetDateTime dateTime) {
        log.info("Event received: message_id={}, event_id={}, courthouse={}, courtroom={}, source={}, date_time={}",
                 messageId, eventId,
                 DataUtil.toUpperCase(courthouse),
                 DataUtil.toUpperCase(courtroom), source, getDateTimeIsoFormatted(dateTime));
    }

    @Override
    public void missingCourthouse(DartsEvent event) {
        log.error("Courthouse not found: message_id={}, event_id={}, courthouse={}, courtroom={}, event_timestamp={}",
                  event.getMessageId(),
                  event.getEventId(),
                  DataUtil.toUpperCase(event.getCourthouse()),
                  DataUtil.toUpperCase(event.getCourtroom()),
                  getDateTimeIsoFormatted(event.getDateTime()));
    }

    @Override
    public void missingNodeRegistry(DartsEvent event) {
        log.error("Unregistered Room: message_id={}, event_id={}, courthouse={}, courtroom={}, event_timestamp={}",
                  event.getMessageId(),
                  event.getEventId(),
                  DataUtil.toUpperCase(event.getCourthouse()),
                  DataUtil.toUpperCase(event.getCourtroom()),
                  getDateTimeIsoFormatted(event.getDateTime()));
    }

    @Override
    public void manualObfuscation(EventEntity eventEntity) {
        log.info(manualObfuscationMessage(eventEntity));
    }

    public static String manualObfuscationMessage(EventEntity eventEntity) {
        return String.format("Event id %s manually obfuscated by user %s", eventEntity.getId(), eventEntity.getLastModifiedBy().getId());
    }
}