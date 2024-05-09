package uk.gov.hmcts.darts.transcriptions.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionWorkflowsResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetYourTranscriptsResponse;
import uk.gov.hmcts.darts.transcriptions.model.RequestTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriberViewSummary;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionStatus;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTranscriberCountsResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionsItem;
import uk.gov.hmcts.darts.transcriptions.service.AdminTranscriptionSearchService;
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
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;


@RestController
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public class TranscriptionController implements TranscriptionApi {

    private final TranscriptionService transcriptionService;
    private final AdminTranscriptionSearchService adminTranscriptionSearchService;
    private final AuthorisationUnitOfWork authorisation;

    private final Validator<TranscriptionRequestDetails> transcriptionRequestDetailsValidator;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<List<GetTranscriptionWorkflowsResponse>> adminTranscriptionWorkflowsGet(Integer transcriptionId, Boolean isCurrent)  {
        return ResponseEntity.ok(transcriptionService.getTranscriptionWorkflows(transcriptionId, isCurrent));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(bodyAuthorisation = true, contextId = ANY_ENTITY_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER},
        globalAccessSecurityRoles = {JUDGE, SUPER_ADMIN, SUPER_USER})
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
    @Authorisation(contextId = TRANSCRIPTION_ID,
        securityRoles = {APPROVER, TRANSCRIBER},
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<UpdateTranscriptionResponse> updateTranscription(Integer transcriptionId,
                                                                           UpdateTranscriptionRequest updateTranscription) {

        return ResponseEntity.ok(transcriptionService.updateTranscription(transcriptionId, updateTranscription, false));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<UpdateTranscriptionAdminResponse> updateTranscriptionAdmin(Integer transcriptionId,
                                                                                     UpdateTranscriptionRequest updateTranscriptionRequest) {
        return ResponseEntity.ok(transcriptionService.updateTranscriptionAdmin(transcriptionId, updateTranscriptionRequest, false));
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
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER},
        globalAccessSecurityRoles = {JUDGE, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS})
    public ResponseEntity<Resource> downloadTranscript(Integer transcriptionId) {
        final DownloadTranscriptResponse downloadTranscriptResponse = transcriptionService.downloadTranscript(
            transcriptionId);
        return ResponseEntity.ok()
            .header(
                CONTENT_DISPOSITION,
                String.format("attachment; filename=\"%s\"", downloadTranscriptResponse.getFileName())
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
        securityRoles = {JUDGE, APPROVER, REQUESTER, TRANSCRIBER},
        globalAccessSecurityRoles = {JUDGE, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS})
    public ResponseEntity<GetTranscriptionByIdResponse> getTranscription(Integer transcriptionId) {
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
                                                       new SecurityRoleEnum[]{JUDGE, REQUESTER, APPROVER},
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

    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(
        contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    @Override
    public ResponseEntity<List<TranscriptionStatus>> getTranscriptionStatus() {
        return ResponseEntity.ok(transcriptionService.getTranscriptionStatuses());
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(bodyAuthorisation = true, contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<List<TranscriptionSearchResponse>> adminTranscriptionsSearchPost(TranscriptionSearchRequest transcriptionSearchRequest) {
        return new ResponseEntity<>(
            adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest),
            HttpStatus.OK
        );
    }


}
