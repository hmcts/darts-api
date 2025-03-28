package uk.gov.hmcts.darts.audio.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audio.component.AudioResponseMapper;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.http.api.AudioApi;
import uk.gov.hmcts.darts.audio.mapper.TransformedMediaMapper;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequestWithStorageGUID;
import uk.gov.hmcts.darts.audio.model.AdminMediaResponse;
import uk.gov.hmcts.darts.audio.model.AdminVersionedMediaResponse;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.model.AudioPreview;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseItem;
import uk.gov.hmcts.darts.audio.model.GetAdminMediasMarkedForDeletionItem;
import uk.gov.hmcts.darts.audio.model.GetTransformedMediaResponse;
import uk.gov.hmcts.darts.audio.model.MediaApproveMarkedForDeletionResponse;
import uk.gov.hmcts.darts.audio.model.MediaHideRequest;
import uk.gov.hmcts.darts.audio.model.MediaHideResponse;
import uk.gov.hmcts.darts.audio.model.PatchAdminMediasByIdRequest;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchRequest;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchResponseItem;
import uk.gov.hmcts.darts.audio.service.AdminMediaService;
import uk.gov.hmcts.darts.audio.service.AudioPreviewService;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioUploadService;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audio.util.StreamingResponseEntityUtil;
import uk.gov.hmcts.darts.audio.validation.AddAudioMetaDataValidator;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.model.PostAdminSearchRequest;
import uk.gov.hmcts.darts.common.util.AdminSearchRequestValidator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus.READY;
import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.HEARING_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.MEDIA_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DARTS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.MID_TIER;
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
    private final AudioUploadService audioUploadService;
    private final AudioResponseMapper audioResponseMapper;
    private final AudioPreviewService audioPreviewService;
    private final AddAudioMetaDataValidator addAudioMetaDataValidator;
    private final MediaRequestService mediaRequestService;
    private final TransformedMediaMapper transformedMediaMapper;
    private final AdminMediaService adminMediaService;
    private final AdminSearchRequestValidator adminSearchRequestValidator;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = HEARING_ID,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA, DARTS})
    public ResponseEntity<List<AudioMetadata>> getAudioMetadata(Integer hearingId) {
        List<MediaEntity> mediaEntities = audioService.getMediaEntitiesByHearingAndChannel(hearingId, 1);
        List<AudioMetadata> audioMetadata = audioResponseMapper.mapToAudioMetadata(mediaEntities);
        audioService.setIsArchived(audioMetadata, hearingId);
        audioService.setIsAvailable(audioMetadata);

        return new ResponseEntity<>(audioMetadata, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {MID_TIER})
    public ResponseEntity<Void> addAudioMetaData(
        @Parameter(name = "AddAudioMetadataRequestWithStorageGUID", description = "") @Valid @RequestBody(required = false)
        AddAudioMetadataRequestWithStorageGUID metadata) {

        // validate the payloads
        addAudioMetaDataValidator.validate(metadata);
        audioUploadService.addAudio(Optional.ofNullable(metadata.getStorageGuid()).map(UUID::toString).orElse(null), metadata);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @SneakyThrows
    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_ID,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA, DARTS})
    public ResponseEntity<byte[]> preview(Integer mediaId, String httpRangeList) {
        AudioPreview audioPreview = audioPreviewService.getOrCreateAudioPreview(mediaId);
        if (audioPreview.getStatus().equals(FAILED)) {
            log.info("Media with ID {} status FAILED", mediaId);
            throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST);
        }
        if (audioPreview.getStatus().equals(READY)) {
            log.info("Media with ID {} status READY", mediaId);
            return StreamingResponseEntityUtil.createResponseEntity(audioPreview.getAudio(), httpRangeList);
        }
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<GetTransformedMediaResponse> adminGetTransformedMedia(Integer transformedMediaId) {
        var transformedMedia = mediaRequestService.getTransformedMediaById(transformedMediaId);
        GetTransformedMediaResponse response = transformedMediaMapper.mapToGetTransformedMediaResponse(transformedMedia);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_USER, SUPER_ADMIN})
    public ResponseEntity<List<GetAdminMediaResponseItem>> getAdminMedias(Integer transformedMediaId, List<Integer> hearingIds, OffsetDateTime startAt,
                                                                          OffsetDateTime endAt) {

        List<GetAdminMediaResponseItem> response = adminMediaService.filterMedias(transformedMediaId, hearingIds, startAt, endAt);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<MediaHideResponse> postAdminHideMediaId(Integer mediaId, MediaHideRequest mediaHideRequest) {
        MediaHideResponse audioResponse = adminMediaService.adminHideOrShowMediaById(mediaId, mediaHideRequest);
        return new ResponseEntity<>(audioResponse, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<MediaApproveMarkedForDeletionResponse> postAdminApproveMediaMarkedForDeletion(Integer mediaId) {
        MediaApproveMarkedForDeletionResponse audioResponse = adminMediaService.adminApproveMediaMarkedForDeletion(mediaId);
        return new ResponseEntity<>(audioResponse, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_USER, SUPER_ADMIN})
    public ResponseEntity<AdminMediaResponse> getAdminMediasById(Integer id) {
        return new ResponseEntity<>(adminMediaService.getMediasById(id), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<AdminVersionedMediaResponse> getAdminMediaVersionsById(Integer id) {
        return new ResponseEntity<>(adminMediaService.getMediaVersionsById(id), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<Void> patchAdminMediasById(Integer id, PatchAdminMediasByIdRequest patchAdminMediasByIdRequest) {
        adminMediaService.patchMediasById(id, patchAdminMediasByIdRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_USER, SUPER_ADMIN})
    public ResponseEntity<List<PostAdminMediasSearchResponseItem>> adminMediasSearchPost(PostAdminMediasSearchRequest adminMediasSearchRequest) {
        adminSearchRequestValidator.validate(PostAdminSearchRequest.builder()
                                                 .caseNumber(adminMediasSearchRequest.getCaseNumber())
                                                 .courthouseIds(adminMediasSearchRequest.getCourthouseIds())
                                                 .hearingStartAt(adminMediasSearchRequest.getHearingStartAt())
                                                 .hearingEndAt(adminMediasSearchRequest.getHearingEndAt())
                                                 .build());

        return new ResponseEntity<>(adminMediaService.performAdminMediasSearchPost(adminMediasSearchRequest), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<List<GetAdminMediasMarkedForDeletionItem>> adminMediasMarkedForDeletionGet() {
        return new ResponseEntity<>(adminMediaService.getMediasMarkedForDeletion(), HttpStatus.OK);
    }

}