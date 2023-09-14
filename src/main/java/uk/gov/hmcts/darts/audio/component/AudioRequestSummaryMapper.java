package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.model.AudioRequestSummary;
import uk.gov.hmcts.darts.audio.model.AudioRequestSummaryResult;

import java.util.List;

public interface AudioRequestSummaryMapper {

    List<AudioRequestSummary> mapToAudioRequestSummary(List<AudioRequestSummaryResult> results);

}
