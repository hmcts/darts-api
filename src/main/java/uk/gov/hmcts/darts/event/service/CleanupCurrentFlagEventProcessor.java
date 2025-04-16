package uk.gov.hmcts.darts.event.service;

@FunctionalInterface
public interface CleanupCurrentFlagEventProcessor {

    void processEvent(Integer eventId);
}