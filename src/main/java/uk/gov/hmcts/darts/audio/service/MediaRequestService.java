package uk.gov.hmcts.darts.audio.service;


import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioNonAccessedResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.GetAudioRequestResponse;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaRequest;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaResponse;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface MediaRequestService {

    AudioNonAccessedResponse countNonAccessedAudioForUser(Integer userId);

    MediaRequestEntity getMediaRequestById(Integer id);

    MediaRequestEntity updateAudioRequestStatus(Integer id, MediaRequestStatus mediaRequestStatus);

    boolean isUserDuplicateAudioRequest(AudioRequestDetails audioRequestDetails);

    MediaRequestEntity saveAudioRequest(AudioRequestDetails audioRequestDetails);

    void scheduleMediaRequestPendingNotification(MediaRequestEntity mediaRequest);

    void deleteAudioRequest(Integer mediaRequestId);

    void deleteTransformedMedia(Integer transformedMediaId);

    Optional<MediaRequestEntity> getOldestMediaRequestByStatus(MediaRequestStatus status);

    GetAudioRequestResponse getAudioRequests(Integer userId, Boolean expired);

    void updateTransformedMediaLastAccessedTimestamp(Integer transformedMediaId);

    void updateTransformedMediaLastAccessedTimestampForMediaRequestId(Integer mediaRequestId);

    InputStream download(Integer transformedMediaId);

    InputStream playback(Integer transformedMediaId);

    MediaRequestEntity updateAudioRequestCompleted(MediaRequestEntity mediaRequestEntity);

    List<SearchTransformedMediaResponse> searchForTransformedMedia(SearchTransformedMediaRequest getTransformedMediaRequest);

    TransformedMediaEntity getTransformedMediaById(Integer id);
}