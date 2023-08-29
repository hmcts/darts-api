package uk.gov.hmcts.darts.audio.component;

import uk.gov.hmcts.darts.audio.service.impl.AudioRequestSummaryResult;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestSummary;

import java.util.List;

public interface AudioRequestSummaryMapper {

    List<AudioRequestSummary> mapToAudioRequestSummary(List<AudioRequestSummaryResult> results);

}
