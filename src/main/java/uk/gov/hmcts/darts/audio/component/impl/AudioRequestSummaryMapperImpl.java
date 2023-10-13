package uk.gov.hmcts.darts.audio.component.impl;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.AudioRequestSummaryMapper;
import uk.gov.hmcts.darts.audio.service.impl.AudioRequestSummaryResult;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestSummary;
import uk.gov.hmcts.darts.audiorequests.model.MediaRequestStatus;

import java.util.List;

@Component
public class AudioRequestSummaryMapperImpl implements AudioRequestSummaryMapper {

    @Override
    public List<AudioRequestSummary> mapToAudioRequestSummary(
        List<AudioRequestSummaryResult> results) {
        return results.stream()
            .map(this::mapToAudioRequestSummary)
            .toList();
    }

    private AudioRequestSummary mapToAudioRequestSummary(AudioRequestSummaryResult result) {
        var audioRequestSummary = new AudioRequestSummary();
        audioRequestSummary.setMediaRequestId(result.mediaRequestId());
        audioRequestSummary.setCaseId(result.caseId());
        audioRequestSummary.setCaseNumber(result.caseNumber());
        audioRequestSummary.setCourthouseName(result.courthouseName());
        audioRequestSummary.setHearingDate(result.hearingDate());
        audioRequestSummary.setMediaRequestStartTs(result.mediaRequestStartTs());
        audioRequestSummary.setMediaRequestEndTs(result.mediaRequestEndTs());
        audioRequestSummary.setMediaRequestExpiryTs(result.mediaRequestExpiryTs());
        audioRequestSummary.setMediaRequestStatus(MediaRequestStatus.fromValue(result.mediaRequestStatus().toString()));
        audioRequestSummary.setLastAccessedTs(result.lastAccessedTs());

        return audioRequestSummary;
    }

}
