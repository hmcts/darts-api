package uk.gov.hmcts.darts.event.service;

public interface RemoveDuplicateEventsProcessor {
    void processEvent(Integer eventId);
}
