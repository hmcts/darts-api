package uk.gov.hmcts.darts.audio.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import uk.gov.hmcts.darts.audio.component.AudioResponseMapper;
import uk.gov.hmcts.darts.audio.http.api.AudioApi;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.util.StreamingResponseEntityUtil;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.HEARING_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.MEDIA_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;


@RestController
@RequiredArgsConstructor
@Slf4j
public class AudioController implements AudioApi {

    private final AudioService audioService;
    private final AudioResponseMapper audioResponseMapper;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = HEARING_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public ResponseEntity<List<AudioMetadata>> getAudioMetadata(Integer hearingId) {
        List<MediaEntity> mediaEntities = audioService.getAudioMetadata(hearingId, 1);
        List<AudioMetadata> audioMetadata = audioResponseMapper.mapToAudioMetadata(mediaEntities);

        return new ResponseEntity<>(audioMetadata, HttpStatus.OK);
    }


    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<Void> addAudio(MultipartFile file, AddAudioMetadataRequest metadata) {
        audioService.addAudio(file, metadata);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @SneakyThrows
    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public ResponseEntity<byte[]> preview(Integer mediaId, String httpRangeList) {
        InputStream audioMediaFile = audioService.preview(mediaId);
        return StreamingResponseEntityUtil.createResponseEntity(audioMediaFile, httpRangeList);
    }


    @RequestMapping(
        method = RequestMethod.GET,
        value = "/audio/preview/{media_id}",
        produces = {"text/event-stream", "application/json+problem"}
    )
    @SneakyThrows
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = MEDIA_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public Flux<ServerSentEvent<ResponseEntity<byte[]>>> previewAlternative(
        @Parameter(name = "media_id", description = "Internal identifier for media", required = true, in = ParameterIn.PATH)
        @PathVariable("media_id") Integer mediaId,
        @Parameter(name = "range", description = "Range header, required for streaming audio.", in = ParameterIn.HEADER)
        @RequestHeader(value = "range", required = false) String range
    ) {
        Flux<ServerSentEvent<ResponseEntity<byte[]>>> work = audioService.getAudioPreviewFlux(mediaId, range);

        Flux<ServerSentEvent<ResponseEntity<byte[]>>> heartBeat = Flux.interval(Duration.ofSeconds(5))
            .map(sequence -> ServerSentEvent.<ResponseEntity<byte[]>>builder()
                .id(String.valueOf(sequence))
                .event("heartbeat")
                .build())
            .takeUntilOther(work);
        return Flux.merge(heartBeat, work);
    }

}
