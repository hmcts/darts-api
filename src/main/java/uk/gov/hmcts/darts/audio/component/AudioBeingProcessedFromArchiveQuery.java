package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.model.AudioBeingProcessedFromArchiveQueryResult;

import java.util.List;

@FunctionalInterface
public interface AudioBeingProcessedFromArchiveQuery {
    List<AudioBeingProcessedFromArchiveQueryResult> getResults(Integer hearingId);
}
