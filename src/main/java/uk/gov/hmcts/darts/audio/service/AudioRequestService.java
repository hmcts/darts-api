package uk.gov.hmcts.darts.audio.service;


import uk.gov.hmcts.darts.audiorequest.model.AudioRequestDetails;

public interface AudioRequestService {

    Integer saveAudioRequest(AudioRequestDetails audioRequestDetails);
}
