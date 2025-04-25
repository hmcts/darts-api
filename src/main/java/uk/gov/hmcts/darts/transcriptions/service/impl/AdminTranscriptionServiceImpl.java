package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.mapper.TranscriptionResponseMapper;
import uk.gov.hmcts.darts.transcriptions.model.AdminApproveDeletionResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDocumentByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentRequest;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentResult;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResult;
import uk.gov.hmcts.darts.transcriptions.service.AdminTranscriptionService;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionSearchQuery;
import uk.gov.hmcts.darts.transcriptions.validator.TranscriptionApproveMarkForDeletionValidator;
import uk.gov.hmcts.darts.transcriptions.validator.TranscriptionDocumentHideOrShowValidator;
import uk.gov.hmcts.darts.usermanagement.service.validation.UserAccountExistsValidator;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.HIDE_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;

@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.CouplingBetweenObjects")//TODO - refactor to reduce coupling when this class is next edited
public class AdminTranscriptionServiceImpl implements AdminTranscriptionService {

    private final TranscriptionSearchQuery transcriptionSearchQuery;

    private final TranscriptionRepository transcriptionRepository;

    private final TranscriptionDocumentRepository transcriptionDocumentRepository;

    private final TranscriptionResponseMapper transcriptionMapper;

    private final UserAccountExistsValidator userAccountExistsValidator;

    private final TranscriptionDocumentHideOrShowValidator transcriptionDocumentHideOrShowValidator;

    private final ObjectAdminActionRepository objectAdminActionRepository;

    private final ObjectHiddenReasonRepository objectHiddenReasonRepository;

    private final UserIdentity userIdentity;

    private final TranscriptionApproveMarkForDeletionValidator transcriptionApproveMarkForDeletionValidator;

    private final AuditApi auditApi;

    @Value("${darts.manual-deletion.enabled:false}")
    @Getter(AccessLevel.PACKAGE)
    private boolean manualDeletionEnabled;

    @Override
    @SuppressWarnings({"PMD.NullAssignment"})
    public List<TranscriptionSearchResponse> searchTranscriptions(TranscriptionSearchRequest request) {
        // If the owner filter is provided, we prefetch the ids of all the transcriptions owned by that owner.
        // This avoids using a sub query in the search query and improves performance.  These ids are then used
        // in the main query.
        List<Integer> transcriptionsIdsForOwner = new ArrayList<>();
        if (request.getOwner() != null) {
            transcriptionsIdsForOwner = transcriptionSearchQuery.findTranscriptionsIdsCurrentlyOwnedBy(request.getOwner());
            if (transcriptionsIdsForOwner.isEmpty()) {
                return emptyList();
            }
            var transcriptionId = request.getTranscriptionId();
            if (transcriptionId != null && !transcriptionsIdsForOwner.contains(transcriptionId)) {
                return emptyList();
            }
        }

        var transcriptionIds = new ArrayList<Integer>();
        if (request.getTranscriptionId() != null) {
            transcriptionIds.add(request.getTranscriptionId());
        } else if (isEmpty(transcriptionsIdsForOwner)) {
            transcriptionIds = null;
        } else {
            transcriptionIds.addAll(transcriptionsIdsForOwner);
        }

        return transcriptionSearchQuery.searchTranscriptions(request, transcriptionIds).stream()
            .map(this::toTranscriptionSearchResponse)
            .toList();
    }

    @Override
    public List<SearchTranscriptionDocumentResponse> searchTranscriptionDocument(SearchTranscriptionDocumentRequest searchTranscriptionDocumentRequest) {
        OffsetDateTime requestedAtFrom = searchTranscriptionDocumentRequest.getRequestedAtFrom()
            != null ? OffsetDateTime.of(searchTranscriptionDocumentRequest.getRequestedAtFrom(), LocalTime.MIN, ZoneOffset.UTC) : null;
        OffsetDateTime requestedAtTo = searchTranscriptionDocumentRequest.getRequestedAtTo()
            != null ? OffsetDateTime.of(searchTranscriptionDocumentRequest.getRequestedAtTo(), LocalTime.MAX, ZoneOffset.UTC) : null;

        List<TranscriptionDocumentResult> results = transcriptionDocumentRepository.findTranscriptionMedia(
            searchTranscriptionDocumentRequest.getCaseNumber(),
            searchTranscriptionDocumentRequest.getCourthouseDisplayName(),
            searchTranscriptionDocumentRequest.getHearingDate(),
            searchTranscriptionDocumentRequest.getRequestedBy(),
            requestedAtFrom,
            requestedAtTo,
            searchTranscriptionDocumentRequest.getIsManualTranscription(),
            searchTranscriptionDocumentRequest.getOwner(),
            userIdentity.userHasGlobalAccess(Set.of(SUPER_ADMIN)) //Only Super admin can not view hidden
        );

        return transcriptionMapper.mapSearchTranscriptionDocumentResults(results);
    }

    @Override
    public List<GetTranscriptionDetailAdminResponse> getTranscriptionsForUser(Integer userId, OffsetDateTime requestedAtFrom) {
        List<GetTranscriptionDetailAdminResponse> detailResponseList = new ArrayList<>();

        // throw an exception if the user does not exist
        userAccountExistsValidator.validate(userId);

        List<TranscriptionEntity> entityList = transcriptionRepository.findTranscriptionForUserOnOrAfterDate(userId, requestedAtFrom);

        for (TranscriptionEntity transcriptionEntity : entityList) {
            detailResponseList.add(transcriptionMapper.mapTransactionEntityToTransactionDetails(transcriptionEntity));
        }

        return detailResponseList;
    }

    private TranscriptionSearchResponse toTranscriptionSearchResponse(TranscriptionSearchResult transcriptionSearchResult) {
        var transcriptionSearchResponse = new TranscriptionSearchResponse();
        transcriptionSearchResponse.setTranscriptionId(transcriptionSearchResult.id());
        transcriptionSearchResponse.setCaseId(transcriptionSearchResult.caseId());
        transcriptionSearchResponse.setCaseNumber(transcriptionSearchResult.caseNumber());
        transcriptionSearchResponse.setCourthouseId(transcriptionSearchResult.courthouseId());
        transcriptionSearchResponse.setHearingDate(transcriptionSearchResult.hearingDate());
        transcriptionSearchResponse.setRequestedAt(transcriptionSearchResult.requestedAt());
        transcriptionSearchResponse.setIsManualTranscription(transcriptionSearchResult.isManualTranscription());
        transcriptionSearchResponse.setTranscriptionStatusId(transcriptionSearchResult.transcriptionStatusId());
        transcriptionSearchResponse.setApprovedAt(transcriptionSearchResult.approvedAt());
        return transcriptionSearchResponse;
    }

    @Override
    public GetTranscriptionDocumentByIdResponse getTranscriptionDocumentById(Integer transcriptionDocument) {
        Optional<TranscriptionDocumentEntity> fndEntity = transcriptionDocumentRepository.findById(transcriptionDocument);
        if (fndEntity.isPresent()) {
            return transcriptionMapper.getSearchByTranscriptionDocumentId(fndEntity.get());
        } else {
            throw new DartsApiException(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND);
        }
    }

    @Transactional
    @Override
    public TranscriptionDocumentHideResponse hideOrShowTranscriptionDocumentById(Integer transcriptionDocumentId,
                                                                                 TranscriptionDocumentHideRequest transcriptionDocumentHideRequest) {
        TranscriptionDocumentHideResponse response;

        IdRequest<TranscriptionDocumentHideRequest> request = new IdRequest<>(transcriptionDocumentHideRequest, transcriptionDocumentId);
        transcriptionDocumentHideOrShowValidator.validate(request);

        Optional<TranscriptionDocumentEntity> transcriptionDocumentEntity
            = transcriptionDocumentRepository.findById(transcriptionDocumentId);
        if (transcriptionDocumentEntity.isPresent()) {
            TranscriptionDocumentEntity documentEntity = transcriptionDocumentEntity.get();

            documentEntity.setHidden(transcriptionDocumentHideRequest.getIsHidden());
            documentEntity = transcriptionDocumentRepository.saveAndFlush(documentEntity);

            if (request.getPayload().getIsHidden()) {
                Optional<ObjectHiddenReasonEntity> objectHiddenReasonEntity =
                    objectHiddenReasonRepository.findById(transcriptionDocumentHideRequest.getAdminAction().getReasonId());

                if (objectHiddenReasonEntity.isEmpty()) {
                    throw new DartsApiException(TranscriptionApiError
                                                    .TRANSCRIPTION_DOCUMENT_HIDE_ACTION_REASON_NOT_FOUND);
                }

                auditApi.record(HIDE_TRANSCRIPTION);

                // on hiding add the relevant hide record
                ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
                objectAdminActionEntity.setObjectHiddenReason(objectHiddenReasonEntity.get());
                objectAdminActionEntity.setTicketReference(transcriptionDocumentHideRequest.getAdminAction().getTicketReference());
                objectAdminActionEntity.setComments(transcriptionDocumentHideRequest.getAdminAction().getComments());
                objectAdminActionEntity.setTranscriptionDocument(documentEntity);
                objectAdminActionEntity.setHiddenBy(userIdentity.getUserAccount());
                objectAdminActionEntity.setHiddenDateTime(OffsetDateTime.now());
                objectAdminActionEntity.setMarkedForManualDeletion(false);
                objectAdminActionEntity.setMarkedForManualDelBy(userIdentity.getUserAccount());
                objectAdminActionEntity.setMarkedForManualDelDateTime(OffsetDateTime.now());

                objectAdminActionEntity = objectAdminActionRepository.saveAndFlush(objectAdminActionEntity);

                response = transcriptionMapper.mapHideOrShowResponse(documentEntity, objectAdminActionEntity);
            } else {
                List<ObjectAdminActionEntity> objectAdminActionEntityLst = objectAdminActionRepository.findByTranscriptionDocumentId(transcriptionDocumentId);

                response = transcriptionMapper.mapHideOrShowResponse(transcriptionDocumentEntity.get(), null);

                for (ObjectAdminActionEntity objectAdminActionEntity : objectAdminActionEntityLst) {
                    auditApi.record(AuditActivity.UNHIDE_TRANSCRIPTION, buildUnhideTranscriptionAdditionalDataString(objectAdminActionEntity));
                    objectAdminActionRepository.deleteById(objectAdminActionEntity.getId());
                }
            }
        } else {
            throw new DartsApiException(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND);
        }

        return response;
    }

    private String buildUnhideTranscriptionAdditionalDataString(ObjectAdminActionEntity objectAdminActionEntity) {
        return "Ticket reference: " + objectAdminActionEntity.getTicketReference() + ", Comments: " + objectAdminActionEntity.getComments();
    }

    @Transactional
    @Override
    public AdminApproveDeletionResponse approveDeletionOfTranscriptionDocumentById(Integer transcriptionDocumentId) {
        if (!this.isManualDeletionEnabled()) {
            throw new DartsApiException(CommonApiError.FEATURE_FLAG_NOT_ENABLED, "Manual deletion is not enabled");
        }

        transcriptionApproveMarkForDeletionValidator.validate(transcriptionDocumentId);

        TranscriptionDocumentEntity transcriptionDocumentEntity = getTranscriptionDocumentEntity(transcriptionDocumentId);

        ObjectAdminActionEntity objectAdminActionEntity = objectAdminActionRepository
            .findByTranscriptionDocumentIdAndObjectHiddenReasonIsNotNullAndObjectHiddenReasonMarkedForDeletionTrue(transcriptionDocumentId)
            .orElseThrow(() -> new DartsApiException(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_DELETE_NOT_SUPPORTED));

        UserAccountEntity userAccount = userIdentity.getUserAccount();
        objectAdminActionEntity.setMarkedForManualDeletion(true);
        objectAdminActionEntity.setMarkedForManualDelBy(userAccount);
        objectAdminActionEntity.setMarkedForManualDelDateTime(OffsetDateTime.now());

        objectAdminActionRepository.save(objectAdminActionEntity);

        auditApi.record(AuditActivity.MANUAL_DELETION, userAccount, objectAdminActionEntity.getId().toString());

        return transcriptionMapper.mapAdminApproveDeletionResponse(transcriptionDocumentEntity, objectAdminActionEntity);
    }

    private TranscriptionDocumentEntity getTranscriptionDocumentEntity(Integer transcriptionDocumentId) {
        return transcriptionDocumentRepository.findById(transcriptionDocumentId)
            .orElseThrow(() -> new DartsApiException(TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND));
    }
}