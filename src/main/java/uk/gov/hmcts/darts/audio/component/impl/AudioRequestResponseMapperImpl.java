package uk.gov.hmcts.darts.audio.component.impl;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.AudioRequestResponseMapper;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.AddAudioResponse;

@Component
public class AudioRequestResponseMapperImpl implements AudioRequestResponseMapper {

    @Override
    public AddAudioResponse mapToAddAudioResponse(MediaRequestEntity audioRequest) {
        var hearing = audioRequest.getHearing();
        var courtCase = hearing.getCourtCase();

        AddAudioResponse addAudioResponse = new AddAudioResponse();
        addAudioResponse.setRequestId(audioRequest.getId());
        addAudioResponse.setCaseId(courtCase.getId());
        addAudioResponse.setCaseNumber(courtCase.getCaseNumber());
        addAudioResponse.setCourthouseName(hearing.getCourtroom().getCourthouse().getCourthouseName());
        addAudioResponse.setDefendants(courtCase.getDefendantStringList());
        addAudioResponse.setHearingDate(hearing.getHearingDate());
        addAudioResponse.setStartTime(audioRequest.getStartTime());
        addAudioResponse.setEndTime(audioRequest.getEndTime());
        return addAudioResponse;
    }

}
