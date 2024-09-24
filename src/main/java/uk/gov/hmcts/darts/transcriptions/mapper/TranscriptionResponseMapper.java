package uk.gov.hmcts.darts.transcriptions.mapper;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingReportingRestrictionsEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.AdminAction;
import uk.gov.hmcts.darts.transcriptions.model.AdminActionResponse;
import uk.gov.hmcts.darts.transcriptions.model.AdminApproveDeletionResponse;
import uk.gov.hmcts.darts.transcriptions.model.AdminMarkedForDeletionResponseItem;
import uk.gov.hmcts.darts.transcriptions.model.CaseResponseDetails;
import uk.gov.hmcts.darts.transcriptions.model.CourthouseResponseDetails;
import uk.gov.hmcts.darts.transcriptions.model.CourtroomResponseDetails;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDocumentByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionWorkflowsResponse;
import uk.gov.hmcts.darts.transcriptions.model.HearingResponseDetails;
import uk.gov.hmcts.darts.transcriptions.model.ReportingRestriction;
import uk.gov.hmcts.darts.transcriptions.model.Requestor;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentResponse;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentResponseCase;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentResponseCourthouse;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentResponseHearing;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentResult;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionResponseDetails;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyDetails;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionWorkflowsComment;
import uk.gov.hmcts.darts.transcriptions.util.TranscriptionUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.darts.transcriptions.util.TranscriptionUtil.getRequestedById;
import static uk.gov.hmcts.darts.transcriptions.util.TranscriptionUtil.getRequestedByName;

@Component
@RequiredArgsConstructor
public class TranscriptionResponseMapper {

    private final HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;

    public List<TranscriptionTypeResponse> mapToTranscriptionTypeResponses(List<TranscriptionTypeEntity> transcriptionTypeEntities) {
        return emptyIfNull(transcriptionTypeEntities).stream()
            .map(this::mapToTranscriptionTypeResponse)
            .collect(toList());
    }

    TranscriptionTypeResponse mapToTranscriptionTypeResponse(TranscriptionTypeEntity transcriptionTypeEntity) {
        TranscriptionTypeResponse transcriptionTypeResponse = new TranscriptionTypeResponse();
        transcriptionTypeResponse.setTranscriptionTypeId(transcriptionTypeEntity.getId());
        transcriptionTypeResponse.setDescription(transcriptionTypeEntity.getDescription());
        return transcriptionTypeResponse;
    }

    public List<TranscriptionUrgencyResponse> mapToTranscriptionUrgencyResponses(
        List<TranscriptionUrgencyEntity> transcriptionUrgencyEntities) {
        return emptyIfNull(transcriptionUrgencyEntities).stream()
            .map(this::mapToTranscriptionUrgencyResponse)
            .collect(toList());
    }

    TranscriptionUrgencyResponse mapToTranscriptionUrgencyResponse(TranscriptionUrgencyEntity transcriptionUrgencyEntity) {
        TranscriptionUrgencyResponse transcriptionUrgencyResponse = new TranscriptionUrgencyResponse();
        transcriptionUrgencyResponse.setTranscriptionUrgencyId(transcriptionUrgencyEntity.getId());
        transcriptionUrgencyResponse.setDescription(transcriptionUrgencyEntity.getDescription());
        transcriptionUrgencyResponse.setPriorityOrder(transcriptionUrgencyEntity.getPriorityOrder());
        return transcriptionUrgencyResponse;
    }

    public List<GetTranscriptionWorkflowsResponse> mapToTranscriptionWorkflowsResponse(
        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities,
        List<TranscriptionCommentEntity> migratedTranscriptionComments
    ) {

        List<GetTranscriptionWorkflowsResponse> result = new ArrayList<>();

        List<GetTranscriptionWorkflowsResponse> responseFromTranscriptionWorkflows = transcriptionWorkflowEntities.stream()
            .map(this::mapToTranscriptionWorkflows)
            .toList();
        result.addAll(responseFromTranscriptionWorkflows);

        var responseFromMigratedComments = migratedTranscriptionComments.stream().map(this::mapMigratedCommentToTranscriptionWorkflows).toList();
        result.addAll(responseFromMigratedComments);

        result.sort(byTranscriptionWorkflowTs().reversed());
        return result;
    }

    private GetTranscriptionWorkflowsResponse mapToTranscriptionWorkflows(TranscriptionWorkflowEntity transcriptionWorkflowEntity) {

        GetTranscriptionWorkflowsResponse transcriptionWorkflowsResponse = new GetTranscriptionWorkflowsResponse();
        transcriptionWorkflowsResponse.setWorkflowActor(transcriptionWorkflowEntity.getWorkflowActor().getId());
        transcriptionWorkflowsResponse.setStatusId(transcriptionWorkflowEntity.getTranscriptionStatus().getId());
        transcriptionWorkflowsResponse.setWorkflowTs(transcriptionWorkflowEntity.getWorkflowTimestamp());
        transcriptionWorkflowsResponse.setComments(mapToTranscriptionComments(transcriptionWorkflowEntity.getTranscriptionComments()));

        return transcriptionWorkflowsResponse;
    }

    private GetTranscriptionWorkflowsResponse mapMigratedCommentToTranscriptionWorkflows(TranscriptionCommentEntity transcriptionComment) {

        GetTranscriptionWorkflowsResponse transcriptionWorkflowsResponse = new GetTranscriptionWorkflowsResponse();
        transcriptionWorkflowsResponse.setWorkflowActor(transcriptionComment.getAuthorUserId());
        transcriptionWorkflowsResponse.setWorkflowTs(transcriptionComment.getCommentTimestamp());
        transcriptionWorkflowsResponse.setComments(mapToTranscriptionComments(List.of(transcriptionComment)));

        return transcriptionWorkflowsResponse;
    }

    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
    private List<TranscriptionWorkflowsComment> mapToTranscriptionComments(List<TranscriptionCommentEntity> transcriptionCommentEntities) {
        List<TranscriptionWorkflowsComment> transcriptionWorkflowsComments = new ArrayList<>();
        for (TranscriptionCommentEntity commentEntity : transcriptionCommentEntities) {
            TranscriptionWorkflowsComment transcriptionWorkflowsComment = new TranscriptionWorkflowsComment();
            transcriptionWorkflowsComment.setComment(commentEntity.getComment());
            transcriptionWorkflowsComment.setCommentedAt(commentEntity.getCommentTimestamp());
            transcriptionWorkflowsComment.setAuthorId(commentEntity.getAuthorUserId());
            transcriptionWorkflowsComments.add(transcriptionWorkflowsComment);
        }
        return transcriptionWorkflowsComments;
    }

    private Comparator<GetTranscriptionWorkflowsResponse> byTranscriptionWorkflowTs() {
        return Comparator.comparing(GetTranscriptionWorkflowsResponse::getWorkflowTs);
    }

    public GetTranscriptionByIdResponse mapToTranscriptionResponse(TranscriptionEntity transcriptionEntity) {
        CourtCaseEntity courtCase = transcriptionEntity.getCourtCase();
        if (isNull(courtCase)) {
            throw new DartsApiException(TranscriptionApiError.TRANSCRIPTION_NOT_FOUND);
        }

        GetTranscriptionByIdResponse transcriptionResponse = new GetTranscriptionByIdResponse();

        transcriptionResponse.setTranscriptionId(transcriptionEntity.getId());
        transcriptionResponse.setCaseId(courtCase.getId());
        transcriptionResponse.setCaseNumber(courtCase.getCaseNumber());
        transcriptionResponse.setCourthouseId(courtCase.getCourthouse().getId());
        transcriptionResponse.setCourthouse(courtCase.getCourthouse().getDisplayName());
        transcriptionResponse.setDefendants(courtCase.getDefendantStringList());
        transcriptionResponse.setJudges(courtCase.getJudgeStringList());
        CourtroomEntity courtroomEntity = transcriptionEntity.getPrimaryOrSecondaryCourtroom();

        if (courtroomEntity != null) {
            transcriptionResponse.setCourtroom(courtroomEntity.getName());
        }
        transcriptionResponse.setTranscriptionObjectId(transcriptionEntity.getLegacyObjectId());

        TranscriptionStatusEntity transcriptionStatusEntity = transcriptionEntity.getTranscriptionStatus();
        if (nonNull(transcriptionEntity.getTranscriptionStatus())) {
            transcriptionResponse.setStatus(transcriptionStatusEntity.getDisplayName());
        }

        transcriptionResponse.setReceived(transcriptionEntity.getCreatedDateTime());

        Requestor requestor = new Requestor();
        requestor.setUserFullName(getRequestedByName(transcriptionEntity));
        requestor.setUserId(getRequestedById(transcriptionEntity));
        transcriptionResponse.setRequestor(requestor);

        transcriptionResponse.setRequestorComments(TranscriptionUtil.getTranscriptionCommentAtStatus(
            transcriptionEntity,
            TranscriptionStatusEnum.REQUESTED
        ));
        transcriptionResponse.setRejectionReason(TranscriptionUtil.getTranscriptionCommentAtStatus(transcriptionEntity, TranscriptionStatusEnum.REJECTED));

        final var latestTranscriptionDocumentEntity = transcriptionEntity.getTranscriptionDocumentEntities()
            .stream()
            .max(comparing(TranscriptionDocumentEntity::getUploadedDateTime));
        latestTranscriptionDocumentEntity.ifPresent(
            transcriptionDocumentEntity -> transcriptionResponse.setTranscriptFileName(transcriptionDocumentEntity.getFileName()));

        if (CollectionUtils.isNotEmpty(transcriptionEntity.getHearings())) {
            HearingEntity hearing = transcriptionEntity.getHearings().get(0);
            transcriptionResponse.setHearingId(hearing.getId());
            transcriptionResponse.setHearingDate(hearing.getHearingDate());
        } else {
            transcriptionResponse.setHearingDate(transcriptionEntity.getHearingDate());
        }
        if (nonNull(transcriptionEntity.getTranscriptionUrgency())) {
            // populate the urgency details
            TranscriptionUrgencyDetails urgencyDetails = new TranscriptionUrgencyDetails();
            urgencyDetails.setPriorityOrder(transcriptionEntity.getTranscriptionUrgency().getPriorityOrder());
            urgencyDetails.setTranscriptionUrgencyId(transcriptionEntity.getTranscriptionUrgency().getId());
            urgencyDetails.setDescription(transcriptionEntity.getTranscriptionUrgency().getDescription());
            transcriptionResponse.setTranscriptionUrgency(urgencyDetails);
        }

        transcriptionResponse.setRequestType(transcriptionEntity.getTranscriptionType().getDescription());
        transcriptionResponse.setTranscriptionStartTs(transcriptionEntity.getStartTime());
        transcriptionResponse.setTranscriptionEndTs(transcriptionEntity.getEndTime());
        transcriptionResponse.setIsManual(transcriptionEntity.getIsManualTranscription());
        transcriptionResponse.setHideRequestFromRequestor(transcriptionEntity.getHideRequestFromRequestor());

        mapReportingRestrictions(courtCase, transcriptionResponse);

        // To be removed when FE updated
        EventHandlerEntity reportingRestriction = courtCase.getReportingRestrictions();
        if (nonNull(reportingRestriction)) {
            transcriptionResponse.setReportingRestriction(reportingRestriction.getEventName());
        }

        return transcriptionResponse;
    }

    private void mapReportingRestrictions(CourtCaseEntity courtCase, GetTranscriptionByIdResponse transcriptionResponse) {
        var reportingRestrictions = hearingReportingRestrictionsRepository.findAllByCaseId(courtCase.getId()).stream()
            .map(this::toReportingRestriction)
            .collect(toList());

        if (courtCase.getReportingRestrictions() != null && reportingRestrictions.isEmpty()) {
            reportingRestrictions.add(
                reportingRestrictionWithName(courtCase.getReportingRestrictions().getEventName()));
        }

        transcriptionResponse.setCaseReportingRestrictions(
            sortedByTimestamp(reportingRestrictions));

        populateReportingRestrictionField(courtCase, transcriptionResponse);
    }

    private static void populateReportingRestrictionField(CourtCaseEntity caseEntity, GetTranscriptionByIdResponse response) {
        if (caseEntity.getReportingRestrictions() != null) {
            response.setReportingRestriction(caseEntity.getReportingRestrictions().getEventName());
        }
    }

    private static List<ReportingRestriction> sortedByTimestamp(List<ReportingRestriction> reportingRestrictions) {
        return reportingRestrictions.stream()
            .sorted(comparing(ReportingRestriction::getEventTs))
            .collect(toList());
    }

    private static ReportingRestriction reportingRestrictionWithName(String name) {
        var reportingRestriction = new ReportingRestriction();
        reportingRestriction.setEventName(name);
        return reportingRestriction;
    }

    private ReportingRestriction toReportingRestriction(HearingReportingRestrictionsEntity restrictionsEntity) {
        var reportingRestriction = new ReportingRestriction();
        reportingRestriction.setEventId(restrictionsEntity.getEveId());
        reportingRestriction.setEventName(restrictionsEntity.getEventName());
        reportingRestriction.setEventText(restrictionsEntity.getEventText());
        reportingRestriction.setHearingId(restrictionsEntity.getHearingId());
        reportingRestriction.setEventTs(restrictionsEntity.getEventDateTime());
        return reportingRestriction;
    }

    public GetTranscriptionDetailAdminResponse mapTransactionEntityToTransactionDetails(TranscriptionEntity transcriptionEntity) {
        GetTranscriptionDetailAdminResponse details = new GetTranscriptionDetailAdminResponse();

        if (transcriptionEntity.getTranscriptionStatus() != null) {
            details.setTranscriptionStatusId(transcriptionEntity.getTranscriptionStatus().getId());
        }

        details.setTranscriptionId(transcriptionEntity.getId());
        details.setIsManualTranscription(transcriptionEntity.getIsManualTranscription());

        if (transcriptionEntity.getCourtCase() != null) {
            details.setCaseNumber(transcriptionEntity.getCourtCase().getCaseNumber());
        }

        if (transcriptionEntity.getHearing() != null) {
            details.setHearingDate(transcriptionEntity.getHearing().getHearingDate());
        }

        Optional<CourthouseEntity> courthouseEntityOptional = transcriptionEntity.getCourtHouse();
        courthouseEntityOptional.ifPresent(courthouseEntity -> details.setCourthouseId(courthouseEntity.getId()));

        details.requestedAt(transcriptionEntity.getCreatedDateTime());
        return details;
    }

    public SearchTranscriptionDocumentResponse mapSearchTranscriptionDocumentResult(TranscriptionDocumentResult transcriptionDocumentResponse) {
        SearchTranscriptionDocumentResponse transformedMediaDetails = new SearchTranscriptionDocumentResponse();
        transformedMediaDetails.setTranscriptionDocumentId(transcriptionDocumentResponse.transcriptionDocumentId());
        transformedMediaDetails.setTranscriptionId(transcriptionDocumentResponse.transcriptionId());

        transformedMediaDetails.isManualTranscription(transcriptionDocumentResponse.isManualTranscription());
        transformedMediaDetails.isHidden(transcriptionDocumentResponse.isHidden());

        // prioritise the case from the hearing
        if (transcriptionDocumentResponse.hearingCaseId() != null) {
            SearchTranscriptionDocumentResponseCase caseResponse = new SearchTranscriptionDocumentResponseCase();
            caseResponse.setId(transcriptionDocumentResponse.hearingCaseId());
            caseResponse.setCaseNumber(transcriptionDocumentResponse.hearingCaseNumber());
            transformedMediaDetails.setCase(caseResponse);
        } else {
            SearchTranscriptionDocumentResponseCase caseResponse = new SearchTranscriptionDocumentResponseCase();
            caseResponse.setId(transcriptionDocumentResponse.caseId());
            caseResponse.setCaseNumber(transcriptionDocumentResponse.caseNumber());
            transformedMediaDetails.setCase(caseResponse);
        }

        // prioritise the courthouse that is connected to the hearing
        if (transcriptionDocumentResponse.hearingCourthouseId() != null) {
            SearchTranscriptionDocumentResponseCourthouse courthouseResponse = new SearchTranscriptionDocumentResponseCourthouse();
            courthouseResponse.setId(transcriptionDocumentResponse.hearingCourthouseId());
            courthouseResponse.setDisplayName(transcriptionDocumentResponse.hearingCourthouseDisplayName());
            transformedMediaDetails.setCourthouse(courthouseResponse);
        } else {
            SearchTranscriptionDocumentResponseCourthouse courthouseResponse = new SearchTranscriptionDocumentResponseCourthouse();
            courthouseResponse.setId(transcriptionDocumentResponse.courthouseId());
            courthouseResponse.setDisplayName(transcriptionDocumentResponse.courthouseDisplayName());
            transformedMediaDetails.setCourthouse(courthouseResponse);
        }

        if (transcriptionDocumentResponse.hearingId() != null) {
            SearchTranscriptionDocumentResponseHearing hearingResponse = new SearchTranscriptionDocumentResponseHearing();
            hearingResponse.setId(transcriptionDocumentResponse.hearingId());
            hearingResponse.setHearingDate(transcriptionDocumentResponse.hearingDate());
            transformedMediaDetails.setHearing(hearingResponse);
        }

        return transformedMediaDetails;
    }

    public List<SearchTranscriptionDocumentResponse> mapSearchTranscriptionDocumentResults(List<TranscriptionDocumentResult> entityList) {
        List<SearchTranscriptionDocumentResponse> mappedDetails = new ArrayList<>();
        for (TranscriptionDocumentResult entity : entityList) {
            mappedDetails.add(mapSearchTranscriptionDocumentResult(entity));
        }

        return mappedDetails;
    }

    public GetTranscriptionDocumentByIdResponse getSearchByTranscriptionDocumentId(TranscriptionDocumentEntity entity) {
        return new GetTranscriptionDocumentByIdResponse()
            .transcriptionDocumentId(entity.getId())
            .transcriptionId(entity.getTranscription().getId())
            .fileName(entity.getFileName())
            .fileType(entity.getFileType())
            .fileSizeBytes(entity.getFileSize())
            .uploadedAt(entity.getUploadedDateTime())
            .uploadedBy(entity.getUploadedBy().getId())
            .isHidden(entity.isHidden())
            .retainUntil(entity.getRetainUntilTs())
            .contentObjectId(entity.getContentObjectId())
            .clipId(entity.getClipId())
            .checksum(entity.getChecksum())
            .lastModifiedAt(entity.getLastModifiedTimestamp())
            .lastModifiedBy(entity.getLastModifiedBy().getId())
            .adminAction(buildAdminActionForHiddenValue(entity));
    }

    private AdminAction buildAdminActionForHiddenValue(TranscriptionDocumentEntity entity) {
        if (entity.getAdminActions().isEmpty() || !entity.isHidden()) {
            return null;
        }

        return buildAdminAction(entity);
    }

    private AdminAction buildAdminAction(TranscriptionDocumentEntity entity) {
        if (entity.getAdminActions().isEmpty()) {
            return null;
        }

        var action = entity.getAdminActions().get(0); // assume only 1 exists
        return new AdminAction()
            .comments(action.getComments())
            .id(action.getId())
            .reasonId(action.getObjectHiddenReason().getId())
            .hiddenById(action.getHiddenBy().getId())
            .hiddenAt(action.getHiddenDateTime())
            .isMarkedForManualDeletion(action.isMarkedForManualDeletion())
            .markedForManualDeletionAt(action.getMarkedForManualDelDateTime())
            .ticketReference(action.getTicketReference())
            .markedForManualDeletionById(action.getMarkedForManualDelBy().getId());

    }

    public TranscriptionDocumentHideResponse mapHideOrShowResponse(TranscriptionDocumentEntity entity, ObjectAdminActionEntity objectAdminActionEntity) {
        TranscriptionDocumentHideResponse response = new TranscriptionDocumentHideResponse();
        response.setId(entity.getId());
        response.setIsHidden(entity.isHidden());

        if (objectAdminActionEntity != null) {
            AdminActionResponse adminActionResponse = mapAdminActionResponse(objectAdminActionEntity);
            response.setAdminAction(adminActionResponse);
        }

        return response;
    }

    public AdminApproveDeletionResponse mapAdminApproveDeletionResponse(TranscriptionDocumentEntity entity, ObjectAdminActionEntity objectAdminActionEntity) {
        AdminApproveDeletionResponse response = new AdminApproveDeletionResponse();
        response.setId(entity.getId());
        response.setIsHidden(entity.isHidden());

        if (objectAdminActionEntity != null) {
            AdminActionResponse adminActionResponse = mapAdminActionResponse(objectAdminActionEntity);
            response.setAdminAction(adminActionResponse);
        }

        return response;
    }

    private AdminActionResponse mapAdminActionResponse(ObjectAdminActionEntity objectAdminActionEntity) {
        AdminActionResponse adminActionResponse = new AdminActionResponse();
        adminActionResponse.setId(objectAdminActionEntity.getId());
        adminActionResponse.setReasonId(objectAdminActionEntity.getObjectHiddenReason().getId());
        adminActionResponse.setHiddenById(objectAdminActionEntity.getHiddenBy().getId());
        adminActionResponse.setHiddenAt(objectAdminActionEntity.getHiddenDateTime());
        adminActionResponse.setIsMarkedForManualDeletion(objectAdminActionEntity.isMarkedForManualDeletion());
        adminActionResponse.setMarkedForManualDeletionById(objectAdminActionEntity.getMarkedForManualDelBy().getId());
        adminActionResponse.setMarkedForManualDeletionAt(objectAdminActionEntity.getMarkedForManualDelDateTime());
        adminActionResponse.setTicketReference(objectAdminActionEntity.getTicketReference());
        adminActionResponse.setComments(objectAdminActionEntity.getComments());

        return adminActionResponse;
    }

    public AdminMarkedForDeletionResponseItem mapTranscriptionDocumentMarkedForDeletion(TranscriptionDocumentEntity transcriptionDocumentEntity) {
        AdminMarkedForDeletionResponseItem response = new AdminMarkedForDeletionResponseItem();

        response.setTranscriptionDocumentId(transcriptionDocumentEntity.getId());

        AdminAction adminAction = buildAdminAction(transcriptionDocumentEntity);

        // if the hearing is null then dont read any associated information
        if (transcriptionDocumentEntity.getTranscription().getHearing() != null) {
            CaseResponseDetails caseResponseDetails = new CaseResponseDetails();
            caseResponseDetails.setId(transcriptionDocumentEntity.getTranscription().getHearing().getCourtCase().getId());
            caseResponseDetails.setCaseNumber(transcriptionDocumentEntity.getTranscription().getHearing().getCourtCase().getCaseNumber());

            HearingEntity hearingEntity = transcriptionDocumentEntity.getTranscription().getHearing();

            CourthouseResponseDetails courthouseResponseDetails = new CourthouseResponseDetails();
            courthouseResponseDetails.setId(hearingEntity.getCourtroom().getCourthouse().getId());
            courthouseResponseDetails.setDisplayName(hearingEntity.getCourtroom().getCourthouse().getDisplayName());

            CourtroomResponseDetails courtroomResponseDetails = new CourtroomResponseDetails();
            courtroomResponseDetails.setId(hearingEntity.getCourtroom().getId());
            courtroomResponseDetails.setName(hearingEntity.getCourtroom().getName());

            HearingResponseDetails hearingResponseDetails = new HearingResponseDetails();
            hearingResponseDetails.setId(transcriptionDocumentEntity.getTranscription().getHearing().getId());
            hearingResponseDetails.setHearingDate(hearingEntity.getHearingDate());

            response.setCourthouse(courthouseResponseDetails);
            response.setCourtroom(courtroomResponseDetails);
            response.setHearing(hearingResponseDetails);
            response.setCase(caseResponseDetails);
        }

        TranscriptionResponseDetails transcriptionResponseDetails = new TranscriptionResponseDetails();
        transcriptionResponseDetails.setId(transcriptionDocumentEntity.getTranscription().getId());

        response.setAdminAction(adminAction);
        response.setTranscription(transcriptionResponseDetails);
        response.setTranscriptionDocumentId(transcriptionDocumentEntity.getId());

        return response;
    }

}