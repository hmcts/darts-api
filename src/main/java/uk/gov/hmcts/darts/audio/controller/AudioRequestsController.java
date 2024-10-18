package uk.gov.hmcts.darts.audio.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.component.AudioRequestResponseMapper;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audio.util.StreamingResponseEntityUtil;
import uk.gov.hmcts.darts.audiorequests.http.api.AudioRequestsApi;
import uk.gov.hmcts.darts.audiorequests.model.AddAudioResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioNonAccessedResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.audiorequests.model.GetAudioRequestResponse;
import uk.gov.hmcts.darts.audiorequests.model.MediaPatchRequest;
import uk.gov.hmcts.darts.audiorequests.model.MediaPatchResponse;
import uk.gov.hmcts.darts.audiorequests.model.MediaRequest;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaRequest;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaResponse;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.DOWNLOAD_HEARING_ID_TRANSCRIBER;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.MEDIA_REQUEST_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.TRANSFORMED_MEDIA_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DARTS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@RestController
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public class AudioRequestsController implements AudioRequestsApi {

    private final MediaRequestService mediaRequestService;

    private final AudioRequestResponseMapper audioRequestResponseMapper;

    private final LogApi logApi;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<AudioNonAccessedResponse> getNonAccessedCount(Integer userId) {
        return new ResponseEntity<>(mediaRequestService.countNonAccessedAudioForUser(userId), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_REQUEST_ID,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA, DARTS})
    public ResponseEntity<Void> deleteAudioRequest(Integer mediaRequestId) {
        mediaRequestService.deleteAudioRequest(mediaRequestId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @SneakyThrows
    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSFORMED_MEDIA_ID,
        securityRoles = {TRANSCRIBER},
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER, DARTS})
    public ResponseEntity<Resource> download(Integer transformedMediaId) {
        DownloadResponseMetaData downloadResponseMetadata = mediaRequestService.download(transformedMediaId);
        return ResponseEntity.ok().body(downloadResponseMetadata.getResource());
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(bodyAuthorisation = true, contextId = DOWNLOAD_HEARING_ID_TRANSCRIBER,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA, DARTS})
    public ResponseEntity<AddAudioResponse> addAudioRequest(AudioRequestDetails audioRequestDetails) {
        if (mediaRequestService.isUserDuplicateAudioRequest(audioRequestDetails)) {
            throw new DartsApiException(AudioRequestsApiError.DUPLICATE_MEDIA_REQUEST);
        }

        AddAudioResponse addAudioResponse;
        MediaRequestEntity audioRequest;

        audioRequest = mediaRequestService.saveAudioRequest(audioRequestDetails);
        addAudioResponse = audioRequestResponseMapper.mapToAddAudioResponse(audioRequest);
        mediaRequestService.scheduleMediaRequestPendingNotification(audioRequest);
        logApi.atsProcessingUpdate(audioRequest);
        return new ResponseEntity<>(addAudioResponse, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(bodyAuthorisation = true, contextId = DOWNLOAD_HEARING_ID_TRANSCRIBER,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA, DARTS})
    public ResponseEntity<AddAudioResponse> addAudioRequestPlayback(AudioRequestDetails audioRequestDetails) {
        audioRequestDetails.setRequestType(AudioRequestType.PLAYBACK);
        return addAudioRequest(audioRequestDetails);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(bodyAuthorisation = true, contextId = DOWNLOAD_HEARING_ID_TRANSCRIBER,
        securityRoles = {TRANSCRIBER},
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER, DARTS})
    public ResponseEntity<AddAudioResponse> addAudioRequestDownload(AudioRequestDetails audioRequestDetails) {
        audioRequestDetails.setRequestType(AudioRequestType.DOWNLOAD);
        return addAudioRequest(audioRequestDetails);
    }

    @SneakyThrows
    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSFORMED_MEDIA_ID,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA, DARTS})
    public ResponseEntity<byte[]> playback(Integer transformedMediaId, String httpRangeList) {
        DownloadResponseMetaData downloadResponseMetadata = mediaRequestService.playback(transformedMediaId);

        return StreamingResponseEntityUtil.createResponseEntity(downloadResponseMetadata.getResource().getInputStream(), httpRangeList);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSFORMED_MEDIA_ID,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA, DARTS})
    public ResponseEntity<Void> updateTransformedMediaLastAccessedTimestamp(Integer transformedMediaId) {
        mediaRequestService.updateTransformedMediaLastAccessedTimestamp(transformedMediaId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSFORMED_MEDIA_ID,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA, DARTS})
    public ResponseEntity<Void> deleteTransformedMedia(Integer transformedMediaId) {
        mediaRequestService.deleteTransformedMedia(transformedMediaId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<GetAudioRequestResponse> getYourAudio(Integer userId, Boolean expired) {
        return new ResponseEntity<>(mediaRequestService.getAudioRequests(userId, expired), HttpStatus.OK);
    }

    @Override
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<List<SearchTransformedMediaResponse>> searchForTransformedMedia(SearchTransformedMediaRequest searchTransformedMediaRequest) {
        List<SearchTransformedMediaResponse> foundTransformedMediaResponse = mediaRequestService.searchRequest(searchTransformedMediaRequest);

        return new ResponseEntity<>(foundTransformedMediaResponse, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<MediaRequest> getMediaRequestById(Integer mediaRequestId) {
        var mediaRequest = mediaRequestService.getMediaRequestById(mediaRequestId);
        return new ResponseEntity<>(mediaRequest, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<MediaPatchResponse> patchMediaRequest(Integer mediaRequestId, MediaPatchRequest mediaPatchRequest) {
        MediaPatchResponse mediaPatchResponse = mediaRequestService.patchMediaRequest(mediaRequestId, mediaPatchRequest);
        return new ResponseEntity<>(mediaPatchResponse, HttpStatus.OK);
    }
}