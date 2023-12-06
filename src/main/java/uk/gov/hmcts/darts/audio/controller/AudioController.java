package uk.gov.hmcts.darts.audio.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audio.component.AudioResponseMapper;
import uk.gov.hmcts.darts.audio.http.api.AudioApi;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.util.StreamingResponseEntityUtil;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.InputStream;
import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.HEARING_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.MEDIA_ID;
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

    private final AudioService audioService;
    private final AudioTransformationService audioTransformationService;
    private final AudioResponseMapper audioResponseMapper;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = HEARING_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public ResponseEntity<List<AudioMetadata>> getAudioMetadata(Integer hearingId) {
        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(hearingId);
        List<AudioMetadata> audioMetadata = audioResponseMapper.mapToAudioMetadata(mediaEntities);

        return new ResponseEntity<>(audioMetadata, HttpStatus.OK);
    }

    @SneakyThrows
    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public ResponseEntity<byte[]> preview(Integer mediaId, String httpRangeList) {
        InputStream audioMediaFile = audioService.preview(mediaId);
        return StreamingResponseEntityUtil.createResponseEntity(audioMediaFile, httpRangeList, mediaId.toString());
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<Void> addAudio(MultipartFile file, AddAudioMetadataRequest metadata) {
        audioService.addAudio(file, metadata);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @RequestMapping(
        method = RequestMethod.GET,
        value = "/audio/keda"
    )

    public ResponseEntity preview2() {
        audioTransformationService.handleKedaInvocationForMediaRequests();
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
