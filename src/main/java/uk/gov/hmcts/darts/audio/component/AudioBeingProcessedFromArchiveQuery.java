package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.model.AudioRequestBeingProcessedFromArchiveQueryResult;

import java.util.List;

public interface AudioBeingProcessedFromArchiveQuery {

    List<AudioRequestBeingProcessedFromArchiveQueryResult> getResults(Integer hearingId);
}
