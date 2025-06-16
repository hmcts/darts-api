package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.event.model.DartsEvent;

@FunctionalInterface
public interface RemoveDuplicateEventsProcessor {
    boolean findAndRemoveDuplicateEvent(DartsEvent event);
}
