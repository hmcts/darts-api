package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioNonAccessedResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.audiorequests.model.GetAudioRequestResponse;
import uk.gov.hmcts.darts.audiorequests.model.MediaPatchRequest;
import uk.gov.hmcts.darts.audiorequests.model.MediaPatchResponse;
import uk.gov.hmcts.darts.audiorequests.model.MediaRequest;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaRequest;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaResponse;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("PMD.TooManyMethods")//TODO - refactor to reduce methods when this class is next edited
public interface MediaRequestService {

    Optional<Integer> retrieveMediaRequestForProcessing(List<Integer> mediaRequestIdsToIgnore);

    AudioNonAccessedResponse countNonAccessedAudioForUser(Integer userId);

    MediaRequestEntity getMediaRequestEntityById(Integer id);

    MediaRequestEntity updateAudioRequestStatus(Integer id, MediaRequestStatus mediaRequestStatus);

    MediaRequestEntity updateAudioRequestStatus(MediaRequestEntity mediaRequestEntity, MediaRequestStatus mediaRequestStatus);

    boolean isUserDuplicateAudioRequest(AudioRequestDetails audioRequestDetails, AudioRequestType audioRequestType, HearingEntity hearing);

    void scheduleMediaRequestPendingNotification(MediaRequestEntity mediaRequest);

    void deleteAudioRequest(Integer mediaRequestId);

    void deleteTransformedMedia(Integer transformedMediaId);

    Optional<MediaRequestEntity> getOldestMediaRequestByStatus(MediaRequestStatus status);

    GetAudioRequestResponse getAudioRequests(Integer userId, Boolean expired);

    void updateTransformedMediaLastAccessedTimestamp(Integer transformedMediaId);

    DownloadResponseMetaData download(Integer transformedMediaId);

    DownloadResponseMetaData playback(Integer transformedMediaId);

    MediaRequestEntity updateAudioRequestCompleted(MediaRequestEntity mediaRequestEntity);

    List<SearchTransformedMediaResponse> searchRequest(SearchTransformedMediaRequest getTransformedMediaRequest);

    TransformedMediaEntity getTransformedMediaById(Integer id);

    MediaRequest getMediaRequestById(Integer mediaRequestId);

    MediaPatchResponse patchMediaRequest(Integer mediaRequestId, MediaPatchRequest request);

    MediaRequestEntity addAudioRequest(AudioRequestDetails audioRequestDetails, AudioRequestType audioRequestType);
}