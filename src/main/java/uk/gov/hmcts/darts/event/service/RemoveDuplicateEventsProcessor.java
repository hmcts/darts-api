package uk.gov.hmcts.darts.event.service;

public interface RemoveDuplicateEventsProcessor {
    boolean processEvent(Integer eventId);
}
