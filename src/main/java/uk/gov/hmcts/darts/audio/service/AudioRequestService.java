package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.dto.AudioRequestDetails;

public interface AudioRequestService {

    void saveAudioRequest(AudioRequestDetails audioRequestDetails);
}
