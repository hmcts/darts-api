package uk.gov.hmcts.darts.audio.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.api.AudioApi;
import uk.gov.hmcts.darts.audio.component.AudioResponseMapper;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AddAudioResponse;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audit.enums.AuditActivityEnum;
import uk.gov.hmcts.darts.audit.service.AuditService;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.InputStream;
import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.HEARING_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.MEDIA_ID;
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
public class AudioController implements AudioApi {

    private final MediaRequestService mediaRequestService;
    private final AudioService audioService;
    private final AudioTransformationService audioTransformationService;
    private final AudioResponseMapper audioResponseMapper;
    private final AuditService auditService;

    @Override
    public ResponseEntity<AddAudioResponse> addAudioRequest(AudioRequestDetails audioRequestDetails) {
        AddAudioResponse addAudioResponse;
        MediaRequestEntity audioRequest;
        try {
            audioRequest = mediaRequestService.saveAudioRequest(audioRequestDetails);
            addAudioResponse = audioResponseMapper.mapToAddAudioResponse(audioRequest);
            auditService.recordAuditRequestAudio(AuditActivityEnum.REQUEST_AUDIO,
                                                 audioRequestDetails.getRequestor(), audioRequestDetails.getHearingId()
            );
        } catch (Exception e) {
            log.error("Failed to request audio", e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        mediaRequestService.scheduleMediaRequestPendingNotification(audioRequest);
        return new ResponseEntity<>(addAudioResponse, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_REQUEST_ID,
        securityRoles = {TRANSCRIBER})
    public ResponseEntity<Resource> download(Integer mediaRequestId) {
        InputStream audioFileStream = audioService.download(mediaRequestId);

        return new ResponseEntity<>(
            new InputStreamResource(audioFileStream),
            HttpStatus.OK
        );
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = HEARING_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS})
    public ResponseEntity<List<AudioMetadata>> getAudioMetadata(Integer hearingId) {
        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(hearingId);
        List<AudioMetadata> audioMetadata = audioResponseMapper.mapToAudioMetadata(mediaEntities);

        return new ResponseEntity<>(audioMetadata, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS})
    public ResponseEntity<org.springframework.core.io.Resource> preview(Integer mediaId) {
        InputStream audioMediaFile = audioService.preview(mediaId);
        return new ResponseEntity<>(new InputStreamResource(audioMediaFile), HttpStatus.OK);
    }


    @Override
    public ResponseEntity<Void> addAudioMetaData(AddAudioMetadataRequest addAudioMetadataRequest) {
        audioService.addAudio(addAudioMetadataRequest);
        return new ResponseEntity<>(HttpStatus.OK);

    }

}
