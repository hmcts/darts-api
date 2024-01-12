package uk.gov.hmcts.darts.audio.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.component.AudioRequestResponseMapper;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audio.util.StreamingResponseEntityUtil;
import uk.gov.hmcts.darts.audiorequests.http.api.AudioRequestsApi;
import uk.gov.hmcts.darts.audiorequests.model.AddAudioResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioNonAccessedResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.GetAudioRequestResponse;
import uk.gov.hmcts.darts.audiorequests.model.GetAudioRequestResponseV1;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;

import java.io.InputStream;
import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.DOWNLOAD_HEARING_ID_TRANSCRIBER;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.MEDIA_REQUEST_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AudioRequestsController implements AudioRequestsApi {

    private final MediaRequestService mediaRequestService;

    private final AudioRequestResponseMapper audioRequestResponseMapper;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<AudioNonAccessedResponse> getNonAccessedCount(Integer userId) {
        return new ResponseEntity<>(mediaRequestService.countNonAccessedAudioForUser(userId), HttpStatus.OK);

    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<List<GetAudioRequestResponseV1>> getYourAudioV1(Integer userId, Boolean expired) {

        return new ResponseEntity<>(mediaRequestService.getAudioRequestsV1(userId, expired), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_REQUEST_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public ResponseEntity<Void> deleteAudioRequest(Integer mediaRequestId) {
        mediaRequestService.deleteAudioRequest(mediaRequestId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_REQUEST_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public ResponseEntity<Void> updateAudioRequestLastAccessedTimestamp(Integer mediaRequestId) {
        mediaRequestService.updateTransformedMediaLastAccessedTimestampForMediaRequestId(mediaRequestId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<Resource> download(Integer transformedMediaId) {
        InputStream audioFileStream = mediaRequestService.download(transformedMediaId);

        return new ResponseEntity<>(
            new InputStreamResource(audioFileStream),
            HttpStatus.OK
        );
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(bodyAuthorisation = true, contextId = DOWNLOAD_HEARING_ID_TRANSCRIBER,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public ResponseEntity<AddAudioResponse> addAudioRequest(AudioRequestDetails audioRequestDetails) {
        AddAudioResponse addAudioResponse;
        MediaRequestEntity audioRequest;

        if (mediaRequestService.isUserDuplicateAudioRequest(audioRequestDetails)) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        try {
            audioRequest = mediaRequestService.saveAudioRequest(audioRequestDetails);
            addAudioResponse = audioRequestResponseMapper.mapToAddAudioResponse(audioRequest);
        } catch (Exception e) {
            log.error("Failed to request audio {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        mediaRequestService.scheduleMediaRequestPendingNotification(audioRequest);
        return new ResponseEntity<>(addAudioResponse, HttpStatus.OK);
    }

    @SneakyThrows
    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<byte[]> playback(Integer transformedMediaId, String httpRangeList) {
        InputStream audioFileStream = mediaRequestService.playback(transformedMediaId);

        return StreamingResponseEntityUtil.createResponseEntity(audioFileStream, httpRangeList);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<Void> updateTransformedMediaLastAccessedTimestamp(Integer transformedMediaId) {
        mediaRequestService.updateTransformedMediaLastAccessedTimestamp(transformedMediaId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<Void> deleteTransformedMedia(Integer transformedMediaId) {
        mediaRequestService.deleteTransformedMedia(transformedMediaId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<GetAudioRequestResponse> getYourAudio(Integer userId, Boolean expired) {
        return new ResponseEntity<>(mediaRequestService.getAudioRequests(userId, expired), HttpStatus.OK);
    }

}
