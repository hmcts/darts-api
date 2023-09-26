package uk.gov.hmcts.darts.audio.service;


import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.service.impl.AudioRequestSummaryResult;

import java.util.List;

public interface MediaRequestService {

    List<MediaRequestEntity> getMediaRequestsByStatus(AudioRequestStatus status);

    MediaRequestEntity getMediaRequestById(Integer id);

    MediaRequestEntity updateAudioRequestStatus(Integer id, AudioRequestStatus audioRequestStatus);

    MediaRequestEntity saveAudioRequest(AudioRequestDetails audioRequestDetails);

    void deleteAudioRequest(Integer mediaRequestId);

    List<AudioRequestSummaryResult> viewAudioRequests(Integer userId, Boolean expired);

}
