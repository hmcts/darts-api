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
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.util.CourtValidationUtils;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.http.api.TranscriptionApi;
import uk.gov.hmcts.darts.transcriptions.model.AdminApproveDeletionResponse;
import uk.gov.hmcts.darts.transcriptions.model.AdminMarkedForDeletionResponseItem;
import uk.gov.hmcts.darts.transcriptions.model.AttachTranscriptResponse;
import uk.gov.hmcts.darts.transcriptions.model.DownloadTranscriptResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDocumentByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionWorkflowsResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetYourTranscriptsResponse;
import uk.gov.hmcts.darts.transcriptions.model.RequestTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentRequest;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriberViewSummary;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideResponse;
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
import uk.gov.hmcts.darts.transcriptions.service.AdminTranscriptionService;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.TRANSCRIPTION_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DARTS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
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
@SuppressWarnings({
    "PMD.CouplingBetweenObjects",//TODO - refactor to reduce coupling when this class is next edited
    "PMD.TooManyMethods"//TODO - refactor to reduce methods when this class is next edited
})
public class TranscriptionController implements TranscriptionApi {

    private final TranscriptionService transcriptionService;
    private final AdminTranscriptionService adminTranscriptionSearchService;
    private final AuthorisationUnitOfWork authorisation;

    private final Validator<TranscriptionRequestDetails> transcriptionRequestDetailsValidator;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<List<GetTranscriptionWorkflowsResponse>> adminTranscriptionWorkflowsGet(Long transcriptionId, Boolean isCurrent) {
        return ResponseEntity.ok(transcriptionService.getTranscriptionWorkflows(transcriptionId, isCurrent));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(bodyAuthorisation = true, contextId = ANY_ENTITY_ID,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, DARTS})
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
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER, DARTS})
    public ResponseEntity<UpdateTranscriptionResponse> updateTranscription(Long transcriptionId,
                                                                           UpdateTranscriptionRequest updateTranscription) {

        return ResponseEntity.ok(transcriptionService.updateTranscription(transcriptionId, updateTranscription, false));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<UpdateTranscriptionAdminResponse> updateTranscriptionAdmin(Long transcriptionId,
                                                                                     UpdateTranscriptionRequest updateTranscriptionRequest) {
        return ResponseEntity.ok(transcriptionService.updateTranscriptionAdmin(transcriptionId, updateTranscriptionRequest, false));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID, securityRoles = {TRANSCRIBER})
    public ResponseEntity<AttachTranscriptResponse> attachTranscript(Long transcriptionId,
                                                                     MultipartFile transcript) {
        return ResponseEntity.ok(transcriptionService.attachTranscript(transcriptionId, transcript));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID,
        securityRoles = {JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, TRANSLATION_QA, RCJ_APPEALS})
    public ResponseEntity<Resource> downloadTranscript(Long transcriptionId) {
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
        securityRoles = {JUDICIARY, APPROVER, REQUESTER, TRANSCRIBER, TRANSLATION_QA},
        globalAccessSecurityRoles = {JUDICIARY, SUPER_ADMIN, SUPER_USER, RCJ_APPEALS, TRANSLATION_QA, DARTS})
    public ResponseEntity<GetTranscriptionByIdResponse> getTranscription(Long transcriptionId) {
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
                                                       new SecurityRoleEnum[]{JUDICIARY, REQUESTER, APPROVER},
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

        if (!CourtValidationUtils.isUppercase(transcriptionSearchRequest.getCourthouseDisplayName())) {
            throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST, "Courthouse display name must be uppercase");
        }

        return new ResponseEntity<>(
            adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest),
            HttpStatus.OK
        );
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<List<GetTranscriptionDetailAdminResponse>> getTranscriptionsForUser(Integer userId, OffsetDateTime requestedAtFrom) {
        return new ResponseEntity<>(adminTranscriptionSearchService.getTranscriptionsForUser(userId, requestedAtFrom),
                                    HttpStatus.OK);
    }

    @Override
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<List<SearchTranscriptionDocumentResponse>> searchForTranscriptionMedia(
        SearchTranscriptionDocumentRequest searchTranscriptionDocumentRequest) {

        List<SearchTranscriptionDocumentResponse> foundTransformedMediaResponse =
            adminTranscriptionSearchService.searchTranscriptionDocument(searchTranscriptionDocumentRequest);

        return new ResponseEntity<>(foundTransformedMediaResponse, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<GetTranscriptionDocumentByIdResponse> getByDocumentId(Long transcriptionDocumentId) {
        return new ResponseEntity<>(adminTranscriptionSearchService.getTranscriptionDocumentById(transcriptionDocumentId),
                                    HttpStatus.OK);

    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<TranscriptionDocumentHideResponse> hideTranscriptionDocumentId(Long transcriptionDocumentId,
                                                                                         TranscriptionDocumentHideRequest transcriptionDocumentHideRequest) {
        TranscriptionDocumentHideResponse response
            = adminTranscriptionSearchService.hideOrShowTranscriptionDocumentById(transcriptionDocumentId, transcriptionDocumentHideRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<AdminApproveDeletionResponse> approveDeletionOfTranscriptionDocumentId(Long transcriptionDocumentId) {
        var response = adminTranscriptionSearchService.approveDeletionOfTranscriptionDocumentById(transcriptionDocumentId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID,
        globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<List<AdminMarkedForDeletionResponseItem>> getTranscriptionDocumentsMarkedForDeletion() {
        List<AdminMarkedForDeletionResponseItem> response
            = transcriptionService.adminGetTranscriptionDocumentsMarkedForDeletion();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}