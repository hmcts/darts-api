package uk.gov.hmcts.darts.audio.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.component.AudioRequestResponseMapper;
import uk.gov.hmcts.darts.audio.component.AudioRequestSummaryMapper;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequests.api.AudioRequestsApi;
import uk.gov.hmcts.darts.audiorequests.model.AddAudioResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioNonAccessedResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestSummary;
import uk.gov.hmcts.darts.audit.enums.AuditActivityEnum;
import uk.gov.hmcts.darts.audit.service.AuditService;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;

import java.io.InputStream;
import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.HEARING_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.MEDIA_REQUEST_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.LANGUAGE_SHOP_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AudioRequestsController implements AudioRequestsApi {

    private final MediaRequestService mediaRequestService;

    private final AudioRequestSummaryMapper audioRequestSummaryMapper;
    private final AudioRequestResponseMapper audioRequestResponseMapper;

    private final AuditService auditService;

    @Override
    public ResponseEntity<AudioNonAccessedResponse> getNonAccessedCount(Integer userId) {
        return new ResponseEntity<>(mediaRequestService.countNonAccessedAudioForUser(userId), HttpStatus.OK);

    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<List<AudioRequestSummary>> getYourAudio(Integer userId, Boolean expired) {

        return new ResponseEntity<>(audioRequestSummaryMapper.mapToAudioRequestSummary(
            mediaRequestService.viewAudioRequests(userId, expired)), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_REQUEST_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS})
    public ResponseEntity<Void> deleteAudioRequest(Integer mediaRequestId) {
        mediaRequestService.deleteAudioRequest(mediaRequestId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_REQUEST_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS})
    public ResponseEntity<Void> updateAudioRequestLastAccessedTimestamp(Integer mediaRequestId) {
        mediaRequestService.updateAudioRequestLastAccessedTimestamp(mediaRequestId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_REQUEST_ID,
        securityRoles = {TRANSCRIBER})
    public ResponseEntity<Resource> download(Integer mediaRequestId) {
        InputStream audioFileStream = mediaRequestService.download(mediaRequestId);

        return new ResponseEntity<>(
            new InputStreamResource(audioFileStream),
            HttpStatus.OK
        );
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(bodyAuthorisation = true, contextId = HEARING_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS})
    public ResponseEntity<AddAudioResponse> addAudioRequest(AudioRequestDetails audioRequestDetails) {
        AddAudioResponse addAudioResponse;
        MediaRequestEntity audioRequest;
        try {
            audioRequest = mediaRequestService.saveAudioRequest(audioRequestDetails);
            addAudioResponse = audioRequestResponseMapper.mapToAddAudioResponse(audioRequest);
            auditService.recordAuditRequestAudio(AuditActivityEnum.REQUEST_AUDIO,
                                                 audioRequestDetails.getRequestor(), audioRequestDetails.getHearingId()
            );
        } catch (Exception e) {
            log.error("Failed to request audio {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        mediaRequestService.scheduleMediaRequestPendingNotification(audioRequest);
        return new ResponseEntity<>(addAudioResponse, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_REQUEST_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS})
    public ResponseEntity<Resource> playback(Integer mediaRequestId) {
        InputStream audioFileStream = mediaRequestService.playback(mediaRequestId);

        return new ResponseEntity<>(
            new InputStreamResource(audioFileStream),
            HttpStatus.OK
        );
    }


}
