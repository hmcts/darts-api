package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.model.AudioBeingProcessedFromArchiveQueryResult;

import java.util.List;

public interface AudioBeingProcessedFromArchiveQuery {

    List<AudioBeingProcessedFromArchiveQueryResult> getResults(Integer hearingId);
}
