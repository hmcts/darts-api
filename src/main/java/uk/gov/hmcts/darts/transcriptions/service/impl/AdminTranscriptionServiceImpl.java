package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.validation.IdRequest;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.mapper.TranscriptionResponseMapper;
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
import uk.gov.hmcts.darts.transcriptions.validator.TranscriptionDocumentHideOrShowValidator;
import uk.gov.hmcts.darts.usermanagement.service.validation.UserAccountExistsValidator;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
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
            searchTranscriptionDocumentRequest.getOwner()
        );

        return transcriptionMapper.mapSearchTranscriptionDocumentResults(results);
    }

    @Override
    public List<GetTranscriptionDetailAdminResponse> getTranscriptionsForUser(Integer userId, OffsetDateTime requestedAtFrom) {
        List<GetTranscriptionDetailAdminResponse> detailResponseList = new ArrayList<>();

        // throw an en exception if the user does not exist
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
        transcriptionSearchResponse.setCaseNumber(transcriptionSearchResult.caseNumber());
        transcriptionSearchResponse.setCourthouseId(transcriptionSearchResult.courthouseId());
        transcriptionSearchResponse.setHearingDate(transcriptionSearchResult.hearingDate());
        transcriptionSearchResponse.setRequestedAt(transcriptionSearchResult.requestedAt());
        transcriptionSearchResponse.setIsManualTranscription(transcriptionSearchResult.isManualTranscription());
        transcriptionSearchResponse.setTranscriptionStatusId(transcriptionSearchResult.transcriptionStatusId());
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

    //@Transactional
    public TranscriptionDocumentHideResponse hideOrShowTranscriptionDocumentById(Integer transcriptionDocumentId,
                                                                                 TranscriptionDocumentHideRequest transcriptionDocumentHideRequest) {

        TranscriptionDocumentHideResponse response;

        IdRequest<TranscriptionDocumentHideRequest> request = new IdRequest<>(transcriptionDocumentHideRequest, transcriptionDocumentId);
        transcriptionDocumentHideOrShowValidator.validate(request);

        Optional<TranscriptionDocumentEntity> transcriptionDocumentEntity
            = transcriptionDocumentRepository.findById(transcriptionDocumentId);
        TranscriptionDocumentEntity documentEntity = transcriptionDocumentEntity.get();

        documentEntity.setHidden(transcriptionDocumentHideRequest.getIsHidden());
        documentEntity = transcriptionDocumentRepository.saveAndFlush(documentEntity);

        ObjectHiddenReasonEntity objectHiddenReasonEntity;
        if (request.getPayload().getIsHidden()) {
            objectHiddenReasonEntity = objectHiddenReasonRepository.findById(transcriptionDocumentHideRequest.getAdminAction().getReasonId()).get();

            // on hiding add the relevant hide record
            ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
            objectAdminActionEntity.setObjectHiddenReason(objectHiddenReasonEntity);
            objectAdminActionEntity.setTicketReference(transcriptionDocumentHideRequest.getAdminAction().getTicketReference());
            objectAdminActionEntity.setComments(transcriptionDocumentHideRequest.getAdminAction().getComments());
            objectAdminActionEntity.setTranscriptionDocument(documentEntity);
            objectAdminActionEntity.setHiddenBy(userIdentity.getUserAccount());
            objectAdminActionEntity.setHiddenDateTime(OffsetDateTime.now());
            objectAdminActionEntity.setMarkedForManualDeletion(false);
            objectAdminActionEntity.setMarkedForManualDelBy(userIdentity.getUserAccount());
            objectAdminActionEntity.setMarkedForManualDelDateTime(OffsetDateTime.now());

            objectAdminActionEntity = objectAdminActionRepository.saveAndFlush(objectAdminActionEntity);
            objectAdminActionEntity = objectAdminActionRepository.findById(objectAdminActionEntity.getId()).get();

            response = transcriptionMapper.mapHideOrShowResponse(documentEntity, objectAdminActionEntity);
        } else {
            List<ObjectAdminActionEntity> objectAdminActionEntityLst = objectAdminActionRepository.findByTranscriptionDocument_Id(transcriptionDocumentId);

            response = transcriptionMapper.mapHideOrShowResponse(transcriptionDocumentEntity.get(), null);

            for (ObjectAdminActionEntity objectAdminActionEntity : objectAdminActionEntityLst) {
                objectAdminActionRepository.deleteById(objectAdminActionEntity.getId());
            }
        }

        return response;
    }
}