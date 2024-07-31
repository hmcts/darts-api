package uk.gov.hmcts.darts.event.service;

import java.util.List;

public interface CleanupCurrentFlagEventProcessor {
    List<Integer> processCurrentEvent();
}