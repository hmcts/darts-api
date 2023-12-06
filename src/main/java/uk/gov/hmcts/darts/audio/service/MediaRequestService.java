package uk.gov.hmcts.darts.audio.service;


import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioNonAccessedResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.GetAudioRequestResponse;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface MediaRequestService {

    AudioNonAccessedResponse countNonAccessedAudioForUser(Integer userId);

    MediaRequestEntity getMediaRequestById(Integer id);

    MediaRequestEntity updateAudioRequestStatus(Integer id, AudioRequestStatus audioRequestStatus);

    MediaRequestEntity saveAudioRequest(AudioRequestDetails audioRequestDetails);

    void scheduleMediaRequestPendingNotification(MediaRequestEntity mediaRequest);

    void deleteAudioRequest(Integer mediaRequestId);

    List<GetAudioRequestResponse> getAudioRequests(Integer userId, Boolean expired);

    Optional<MediaRequestEntity> getOldestMediaRequestByStatus(AudioRequestStatus status);

    void updateTransformedMediaLastAccessedTimestamp(Integer mediaRequestId);

    InputStream download(Integer mediaRequestId);

    InputStream playback(Integer mediaRequestId);

    MediaRequestEntity updateAudioRequestCompleted(MediaRequestEntity mediaRequestEntity, String fileName, AudioRequestOutputFormat audioRequestOutputFormat);
}
