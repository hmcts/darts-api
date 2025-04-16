package uk.gov.hmcts.darts.event.service;

@FunctionalInterface
public interface RemoveDuplicateEventsProcessor {
    boolean findAndRemoveDuplicateEvent(Integer eventId);
}
