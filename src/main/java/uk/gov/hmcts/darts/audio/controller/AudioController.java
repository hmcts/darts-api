package uk.gov.hmcts.darts.audio.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audio.component.AudioResponseMapper;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.http.api.AudioApi;
import uk.gov.hmcts.darts.audio.mapper.TransformedMediaMapper;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.model.AudioPreview;
import uk.gov.hmcts.darts.audio.model.GetTransformedMediaResponse;
import uk.gov.hmcts.darts.audio.service.AudioPreviewService;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audio.util.StreamingResponseEntityUtil;
import uk.gov.hmcts.darts.audio.validation.AddAudioFileValidator;
import uk.gov.hmcts.darts.audio.validation.AddAudioMetaDataValidator;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.List;

import static uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus.READY;
import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.HEARING_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.MEDIA_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
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
public class AudioController implements AudioApi {

    private final AudioService audioService;
    private final AudioResponseMapper audioResponseMapper;
    private final AudioPreviewService audioPreviewService;
    private final AddAudioMetaDataValidator addAudioMetaDataValidator;
    private final AddAudioFileValidator multipartFileValidator;
    private final MediaRequestService mediaRequestService;
    private final TransformedMediaMapper transformedMediaMapper;


    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = HEARING_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDGE, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA})
    public ResponseEntity<List<AudioMetadata>> getAudioMetadata(Integer hearingId) {
        List<MediaEntity> mediaEntities = audioService.getAudioMetadata(hearingId, 1);
        List<AudioMetadata> audioMetadata = audioResponseMapper.mapToAudioMetadata(mediaEntities);
        audioService.setIsArchived(audioMetadata, hearingId);
        audioService.setIsAvailable(audioMetadata);

        return new ResponseEntity<>(audioMetadata, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<Void> addAudio(MultipartFile file, AddAudioMetadataRequest metadata) {

        // validate the payloads
        addAudioMetaDataValidator.validate(metadata);
        multipartFileValidator.validate(file);

        audioService.addAudio(file, metadata);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @SneakyThrows
    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDGE, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA})
    public ResponseEntity<byte[]> preview(Integer mediaId, String httpRangeList) {
        AudioPreview audioPreview = audioPreviewService.getOrCreateAudioPreview(mediaId);
        if (audioPreview.getStatus().equals(FAILED)) {
            log.info("Media with ID {} status FAILED", mediaId);
            throw new DartsApiException(AudioApiError.MEDIA_NOT_FOUND);
        }
        if (audioPreview.getStatus().equals(READY)) {
            log.info("Media with ID {} status READY", mediaId);
            return StreamingResponseEntityUtil.createResponseEntity(audioPreview.getAudio(), httpRangeList);
        }
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<GetTransformedMediaResponse> adminGetTransformedMedia(Integer transformedMediaId) {
        var transformedMedia = mediaRequestService.getTransformedMediaById(transformedMediaId);
        GetTransformedMediaResponse response = transformedMediaMapper.mapToGetTransformedMediaResponse(transformedMedia);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}