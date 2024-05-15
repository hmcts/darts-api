package uk.gov.hmcts.darts.transcriptions.mapper;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingReportingRestrictionsEntity;
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
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionWorkflowsResponse;
import uk.gov.hmcts.darts.transcriptions.model.ReportingRestriction;
import uk.gov.hmcts.darts.transcriptions.model.Requestor;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyDetails;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionWorkflowsComment;
import uk.gov.hmcts.darts.transcriptions.util.TranscriptionUtil;

import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

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
        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities) {
        return emptyIfNull(transcriptionWorkflowEntities).stream()
            .map(this::mapToTranscriptionWorkflows)
            .collect(toList());
    }

    private GetTranscriptionWorkflowsResponse mapToTranscriptionWorkflows(TranscriptionWorkflowEntity transcriptionWorkflowEntity) {

        GetTranscriptionWorkflowsResponse transcriptionWorkflowsResponse = new GetTranscriptionWorkflowsResponse();
        transcriptionWorkflowsResponse.setWorkflowActor(transcriptionWorkflowEntity.getWorkflowActor().getId());
        transcriptionWorkflowsResponse.setStatusId(transcriptionWorkflowEntity.getTranscriptionStatus().getId());
        transcriptionWorkflowsResponse.setWorkflowTs(transcriptionWorkflowEntity.getWorkflowTimestamp());
        transcriptionWorkflowsResponse.setComments(mapToTranscriptionComments(transcriptionWorkflowEntity.getTranscriptionComments()));

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
        transcriptionResponse.setCourthouse(courtCase.getCourthouse().getCourthouseName());
        transcriptionResponse.setDefendants(courtCase.getDefendantStringList());
        transcriptionResponse.setJudges(courtCase.getJudgeStringList());

        TranscriptionStatusEntity transcriptionStatusEntity = transcriptionEntity.getTranscriptionStatus();
        if (nonNull(transcriptionEntity.getTranscriptionStatus())) {
            transcriptionResponse.setStatus(transcriptionStatusEntity.getDisplayName());
        }

        transcriptionResponse.setReceived(transcriptionEntity.getCreatedDateTime());

        Requestor requestor = new Requestor();
        requestor.setUserFullName(getRequestorName(transcriptionEntity));
        requestor.setUserId(getRequestorId(transcriptionEntity));
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
        reportingRestriction.setEventId(restrictionsEntity.getEventId());
        reportingRestriction.setEventName(restrictionsEntity.getEventName());
        reportingRestriction.setEventText(restrictionsEntity.getEventText());
        reportingRestriction.setHearingId(restrictionsEntity.getHearingId());
        reportingRestriction.setEventTs(restrictionsEntity.getEventDateTime());
        return reportingRestriction;
    }

    private String getRequestorName(TranscriptionEntity transcriptionEntity) {
        if (transcriptionEntity.getCreatedBy() != null) {
            return transcriptionEntity.getCreatedBy().getUserName();
        } else {
            return transcriptionEntity.getRequestor();
        }
    }

    private Integer getRequestorId(TranscriptionEntity transcriptionEntity) {
        if (transcriptionEntity.getCreatedBy() != null) {
            return transcriptionEntity.getCreatedBy().getId();
        } else {
            return null;
        }
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
            details.setCourthouseId(transcriptionEntity.getHearing().getCourtroom().getCourthouse().getId());
            details.setHearingDate(transcriptionEntity.getHearing().getHearingDate());
        } else if (transcriptionEntity.getCourtCase() != null) {
            details.setCourthouseId(transcriptionEntity.getCourtCase().getCourthouse().getId());
        }

        details.requestedAt(transcriptionEntity.getCreatedDateTime());
        return details;
    }
}