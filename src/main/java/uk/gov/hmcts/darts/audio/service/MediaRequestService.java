package uk.gov.hmcts.darts.audio.service;


import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioNonAccessedResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.GetAudioRequestResponse;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface MediaRequestService {

    AudioNonAccessedResponse countNonAccessedAudioForUser(Integer userId);

    MediaRequestEntity getMediaRequestById(Integer id);

    MediaRequestEntity updateAudioRequestStatus(Integer id, MediaRequestStatus mediaRequestStatus);

    MediaRequestEntity saveAudioRequest(AudioRequestDetails audioRequestDetails);

    void scheduleMediaRequestPendingNotification(MediaRequestEntity mediaRequest);

    void deleteAudioRequest(Integer mediaRequestId);

    void deleteTransformedMedia(Integer transformedMediaId);

    List<GetAudioRequestResponse> getAudioRequests(Integer userId, Boolean expired);

    Optional<MediaRequestEntity> getOldestMediaRequestByStatus(MediaRequestStatus status);

    void updateTransformedMediaLastAccessedTimestamp(Integer mediaRequestId);

    @Transactional
    void updateTransformedMediaLastAccessedTimestampForMediaRequestId(Integer mediaRequestId);

    InputStream download(Integer mediaRequestId);

    InputStream playback(Integer mediaRequestId);

    MediaRequestEntity updateAudioRequestCompleted(MediaRequestEntity mediaRequestEntity, String fileName, AudioRequestOutputFormat audioRequestOutputFormat);
}
