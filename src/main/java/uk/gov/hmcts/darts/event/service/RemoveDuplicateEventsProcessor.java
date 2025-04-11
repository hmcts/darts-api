package uk.gov.hmcts.darts.event.service;

public interface RemoveDuplicateEventsProcessor {
    boolean findAndRemoveDuplicateEvent(Integer eventId, Integer eveId);
}
