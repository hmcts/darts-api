package uk.gov.hmcts.darts.audio.service;


import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audiorequest.model.AudioRequestDetails;

public interface MediaRequestService {

    MediaRequestEntity getMediaRequestById(Integer id);

    MediaRequestEntity updateAudioRequestStatus(Integer id, AudioRequestStatus audioRequestStatus);

    Integer saveAudioRequest(AudioRequestDetails audioRequestDetails);

}
