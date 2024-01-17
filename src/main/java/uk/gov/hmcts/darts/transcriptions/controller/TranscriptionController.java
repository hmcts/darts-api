package uk.gov.hmcts.darts.transcriptions.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.authorisation.util.AuthorisationUnitOfWork;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.transcriptions.http.api.TranscriptionApi;
import uk.gov.hmcts.darts.transcriptions.model.AttachTranscriptResponse;
import uk.gov.hmcts.darts.transcriptions.model.DownloadTranscriptResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetYourTranscriptsResponse;
import uk.gov.hmcts.darts.transcriptions.model.RequestTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriberViewSummary;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTranscriberCountsResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionsItem;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.TRANSCRIPTION_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;


@RestController
@RequiredArgsConstructor
@Slf4j
public class TranscriptionController implements TranscriptionApi {

    private final TranscriptionService transcriptionService;
    private final AuthorisationUnitOfWork authorisation;

    private final Validator<TranscriptionRequestDetails> transcriptionRequestDetailsValidator;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(bodyAuthorisation = true, contextId = ANY_ENTITY_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public ResponseEntity<RequestTranscriptionResponse> requestTranscription(TranscriptionRequestDetails transcriptionRequestDetails) {
        transcriptionRequestDetailsValidator.validate(transcriptionRequestDetails);

        RequestTranscriptionResponse requestTranscriptionResponse;
        try {
            requestTranscriptionResponse = transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails, true);
        } catch (EntityNotFoundException exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(requestTranscriptionResponse, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID, securityRoles = {APPROVER, TRANSCRIBER})
    public ResponseEntity<UpdateTranscriptionResponse> updateTranscription(Integer transcriptionId,
                                                                           UpdateTranscription updateTranscription) {

        return ResponseEntity.ok(transcriptionService.updateTranscription(transcriptionId, updateTranscription));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID, securityRoles = {TRANSCRIBER})
    public ResponseEntity<AttachTranscriptResponse> attachTranscript(Integer transcriptionId,
                                                                     MultipartFile transcript) {
        return ResponseEntity.ok(transcriptionService.attachTranscript(transcriptionId, transcript));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public ResponseEntity<Resource> downloadTranscript(Integer transcriptionId) {
        final DownloadTranscriptResponse downloadTranscriptResponse = transcriptionService.downloadTranscript(
            transcriptionId);
        return ResponseEntity.ok()
            .header(
                CONTENT_DISPOSITION,
                String.format("attachment; filename=\"%s\"", downloadTranscriptResponse.getFileName())
            )
            .header(
                "external_location",
                downloadTranscriptResponse.getExternalLocation().toString()
            )
            .header(
                "transcription_document_id",
                String.valueOf(downloadTranscriptResponse.getTranscriptionDocumentId())
            )
            .contentType(MediaType.parseMediaType(downloadTranscriptResponse.getContentType()))
            .body(downloadTranscriptResponse.getResource());
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<List<TranscriptionTypeResponse>> getTranscriptionTypes() {
        return new ResponseEntity<>(
            transcriptionService.getTranscriptionTypes(),
            HttpStatus.OK
        );
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<List<TranscriptionUrgencyResponse>> getTranscriptionUrgencies() {
        return new ResponseEntity<>(
            transcriptionService.getTranscriptionUrgenciesByDisplayState(),
            HttpStatus.OK
        );
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<GetYourTranscriptsResponse> getYourTranscripts(Integer userId) {
        return ResponseEntity.ok(transcriptionService.getYourTranscripts(userId, false));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<List<TranscriberViewSummary>> getTranscriberTranscripts(Integer userId, Boolean assigned) {
        return ResponseEntity.ok(transcriptionService.getTranscriberTranscripts(userId, assigned));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID,
        securityRoles = {JUDGE, APPROVER, REQUESTER, TRANSCRIBER, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public ResponseEntity<GetTranscriptionByIdResponse> getTranscription(
        @Parameter(name = "transcription_id", description = "transcription_id is the internal id of the transcription.", required = true,
            in = ParameterIn.PATH) @PathVariable("transcription_id") Integer transcriptionId
    ) {
        return new ResponseEntity<>(
            transcriptionService.getTranscription(transcriptionId),
            HttpStatus.OK
        );
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<List<UpdateTranscriptionsItem>> updateTranscriptions(List<UpdateTranscriptionsItem> request) {
        List<UpdateTranscriptionsItem> responseList = new ArrayList<>();

        Runnable executeOnAuth = () -> {
            responseList.addAll(transcriptionService.updateTranscriptions(request));
        };

        // we authorise the transcription ids
        authorisation.authoriseWithIdsForTranscription(request,
                                                       e -> e.getTranscriptionId().toString(),
                                                       new SecurityRoleEnum[]{JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS},
                                                       executeOnAuth
        );

        return new ResponseEntity<>(
            responseList,
            HttpStatus.OK
        );
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<TranscriptionTranscriberCountsResponse> getTranscriptionTranscriberCounts(Integer userId) {
        return ResponseEntity.ok(transcriptionService.getTranscriptionTranscriberCounts(userId));
    }
}
