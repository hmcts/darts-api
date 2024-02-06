package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.model.AudioRequestBeingProcessedFromArchiveQueryResult;

import java.util.List;

public interface AudioRequestBeingProcessedFromArchiveQuery {

    List<AudioRequestBeingProcessedFromArchiveQueryResult> getResults(Integer mediaRequestId);

    List<AudioRequestBeingProcessedFromArchiveQueryResult> getResultsByMediaIds(List<Integer> mediaIds);
}
