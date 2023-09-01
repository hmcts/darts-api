package uk.gov.hmcts.darts.audio.service;


import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.service.impl.AudioRequestSummaryResult;

import java.util.List;

public interface MediaRequestService {

    MediaRequestEntity getMediaRequestById(Integer id);

    MediaRequestEntity updateAudioRequestStatus(Integer id, AudioRequestStatus audioRequestStatus);

    Integer saveAudioRequest(AudioRequestDetails audioRequestDetails);

    void deleteAudioRequest(Integer mediaRequestId);

    List<AudioRequestSummaryResult> viewAudioRequests(Integer userId, Boolean expired);

}
