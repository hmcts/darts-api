package uk.gov.hmcts.darts.audio.service;


import uk.gov.hmcts.darts.audio.entity.MediaRequest;
import uk.gov.hmcts.darts.audiorequest.model.AudioRequestDetails;

public interface MediaRequestService {

    MediaRequest getMediaRequestById(Integer id);

    Integer saveAudioRequest(AudioRequestDetails audioRequestDetails);

}
