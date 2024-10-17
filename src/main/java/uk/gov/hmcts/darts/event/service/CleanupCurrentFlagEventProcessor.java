package uk.gov.hmcts.darts.event.service;

public interface CleanupCurrentFlagEventProcessor {

    void processEvent(Integer eventId);
}