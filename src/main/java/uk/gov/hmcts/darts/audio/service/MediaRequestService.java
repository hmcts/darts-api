package uk.gov.hmcts.darts.audio.service;


import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.service.impl.AudioRequestSummaryResult;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface MediaRequestService {

    MediaRequestEntity getMediaRequestById(Integer id);

    MediaRequestEntity updateAudioRequestStatus(Integer id, AudioRequestStatus audioRequestStatus);

    MediaRequestEntity saveAudioRequest(AudioRequestDetails audioRequestDetails);

    void scheduleMediaRequestPendingNotification(MediaRequestEntity mediaRequest);

    void deleteAudioRequest(Integer mediaRequestId);

    List<AudioRequestSummaryResult> viewAudioRequests(Integer userId, Boolean expired);

    Optional<MediaRequestEntity> getOldestMediaRequestByStatus(AudioRequestStatus status);

    void updateAudioRequestLastAccessedTimestamp(Integer mediaRequestId);

    InputStream download(Integer mediaRequestId);

}
