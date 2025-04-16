package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.model.AudioRequestBeingProcessedFromArchiveQueryResult;

import java.util.List;

@FunctionalInterface
public interface AudioRequestBeingProcessedFromArchiveQuery {
    List<AudioRequestBeingProcessedFromArchiveQueryResult> getResults(Integer mediaRequestId);
}
