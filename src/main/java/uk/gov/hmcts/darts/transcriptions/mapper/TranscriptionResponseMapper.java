package uk.gov.hmcts.darts.transcriptions.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingReportingRestrictionsEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.ReportingRestriction;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;
import uk.gov.hmcts.darts.transcriptions.util.TranscriptionUtil;

import java.util.List;
import java.util.stream.Collectors;

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
            .collect(Collectors.toList());
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
            .collect(Collectors.toList());
    }

    TranscriptionUrgencyResponse mapToTranscriptionUrgencyResponse(TranscriptionUrgencyEntity transcriptionUrgencyEntity) {
        TranscriptionUrgencyResponse transcriptionUrgencyResponse = new TranscriptionUrgencyResponse();
        transcriptionUrgencyResponse.setTranscriptionUrgencyId(transcriptionUrgencyEntity.getId());
        transcriptionUrgencyResponse.setDescription(transcriptionUrgencyEntity.getDescription());
        transcriptionUrgencyResponse.setPriorityOrder(transcriptionUrgencyEntity.getPriorityOrder());
        return transcriptionUrgencyResponse;
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
        transcriptionResponse.setCourthouse(courtCase.getCourthouse().getCourthouseName());
        transcriptionResponse.setDefendants(courtCase.getDefendantStringList());
        transcriptionResponse.setJudges(courtCase.getJudgeStringList());

        TranscriptionStatusEntity transcriptionStatusEntity = transcriptionEntity.getTranscriptionStatus();
        if (nonNull(transcriptionEntity.getTranscriptionStatus())) {
            transcriptionResponse.setStatus(transcriptionStatusEntity.getDisplayName());
        }

        transcriptionResponse.setFrom(getRequestorName(transcriptionEntity));
        transcriptionResponse.setReceived(transcriptionEntity.getCreatedDateTime());
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

        if (nonNull(transcriptionEntity.getHearing())) {
            transcriptionResponse.setHearingId(transcriptionEntity.getHearing().getId());
            transcriptionResponse.setHearingDate(transcriptionEntity.getHearing().getHearingDate());
        } else {
            transcriptionResponse.setHearingDate(transcriptionEntity.getHearingDate());
        }
        if (nonNull(transcriptionEntity.getTranscriptionUrgency())) {
            transcriptionResponse.setUrgency(transcriptionEntity.getTranscriptionUrgency().getDescription());
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
}
