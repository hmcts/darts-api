package uk.gov.hmcts.darts.audio.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.api.AudioApi;
import uk.gov.hmcts.darts.audio.component.AudioResponseMapper;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audiorequests.model.AddAudioResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.IOException;
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

    public static final int BYTE_RANGE = 128; // increase the byte range from here

    private final AudioService audioService;
    private final AudioTransformationService audioTransformationService;
    private final AudioResponseMapper audioResponseMapper;

    // TODO Used where audio was moved to audio-requests and should be removed when frontend is updated
    private final AudioRequestsController audioRequestsController;

    public ResponseEntity<AddAudioResponse> addAudioRequest(AudioRequestDetails audioRequestDetails) {
        return audioRequestsController.addAudioRequest(audioRequestDetails);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_REQUEST_ID,
        securityRoles = {TRANSCRIBER})
    public ResponseEntity<Resource> download(Integer mediaRequestId) {
        return audioRequestsController.download(mediaRequestId);
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
    public ResponseEntity<byte[]> preview(Integer mediaId, @RequestHeader(value = "Range", required = false) String httpRangeList) {
        InputStream audioMediaFile = audioService.preview(mediaId);
        try {
            byte[] bytes = IOUtils.toByteArray(audioMediaFile);
            return ResponseEntity.status(HttpStatus.OK)
                .header("Content-Type", "audio/mpeg")
                .header("Content-Length", String.valueOf(bytes.length))
                .body(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<Void> addAudioMetaData(AddAudioMetadataRequest addAudioMetadataRequest) {
        audioService.addAudio(addAudioMetadataRequest);
        return new ResponseEntity<>(HttpStatus.OK);

    }

}
