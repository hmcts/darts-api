package uk.gov.hmcts.darts.transcriptions.mapper;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@UtilityClass
public class TranscriptionResponseMapper {

    public List<TranscriptionTypeResponse> mapToTranscriptionTypeResponses(List<TranscriptionTypeEntity> transcriptionTypeEntities) {
        return emptyIfNull(transcriptionTypeEntities).stream()
            .map(TranscriptionResponseMapper::mapToTranscriptionTypeResponse)
            .collect(Collectors.toList());
    }

    TranscriptionTypeResponse mapToTranscriptionTypeResponse(TranscriptionTypeEntity transcriptionTypeEntity) {
        TranscriptionTypeResponse transcriptionTypeResponse = new TranscriptionTypeResponse();
        transcriptionTypeResponse.setTrtId(transcriptionTypeEntity.getId());
        transcriptionTypeResponse.setDescription(transcriptionTypeEntity.getDescription());
        return transcriptionTypeResponse;
    }

    public List<TranscriptionUrgencyResponse> mapToTranscriptionUrgencyResponses(
        List<TranscriptionUrgencyEntity> transcriptionUrgencyEntities) {
        return emptyIfNull(transcriptionUrgencyEntities).stream()
            .map(TranscriptionResponseMapper::mapToTranscriptionUrgencyResponse)
            .collect(Collectors.toList());
    }

    TranscriptionUrgencyResponse mapToTranscriptionUrgencyResponse(TranscriptionUrgencyEntity transcriptionUrgencyEntity) {
        TranscriptionUrgencyResponse transcriptionUrgencyResponse = new TranscriptionUrgencyResponse();
        transcriptionUrgencyResponse.setTruId(transcriptionUrgencyEntity.getId());
        transcriptionUrgencyResponse.setDescription(transcriptionUrgencyEntity.getDescription());
        return transcriptionUrgencyResponse;
    }

    public static GetTranscriptionByIdResponse mapToTranscriptionResponse(TranscriptionEntity transcriptionEntity) {

        GetTranscriptionByIdResponse transcriptionResponse = new GetTranscriptionByIdResponse();
        try {
            CourtCaseEntity courtCase = transcriptionEntity.getCourtCase();
            transcriptionResponse.setTranscriptionId(transcriptionEntity.getId());
            transcriptionResponse.setCaseId(courtCase.getId());
            transcriptionResponse.setCaseNumber(courtCase.getCaseNumber());
            transcriptionResponse.setCourthouse(courtCase.getCourthouse().getCourthouseName());
            transcriptionResponse.setDefendants(courtCase.getDefendantStringList());
            transcriptionResponse.setJudges(courtCase.getJudgeStringList());

            if (transcriptionEntity.getTranscriptionStatus() != null) {
                transcriptionResponse.setStatus(transcriptionEntity.getTranscriptionStatus().getDisplayName());
            }

            transcriptionResponse.setFrom(getRequestorName(transcriptionEntity));
            transcriptionResponse.setReceived(transcriptionEntity.getCreatedDateTime());
            transcriptionResponse.setRequestorComments(getTranscriptionCommentAtStatus(transcriptionEntity, TranscriptionStatusEnum.REQUESTED));
            transcriptionResponse.setRejectionReason(getTranscriptionCommentAtStatus(transcriptionEntity, TranscriptionStatusEnum.REJECTED));

            final var latestTranscriptionDocumentEntity = transcriptionEntity.getTranscriptionDocumentEntities()
                .stream()
                .max(comparing(TranscriptionDocumentEntity::getUploadedDateTime));
            latestTranscriptionDocumentEntity.ifPresent(
                transcriptionDocumentEntity -> transcriptionResponse.setTranscriptFileName(transcriptionDocumentEntity.getFileName()));

            if (transcriptionEntity.getHearing() != null) {
                transcriptionResponse.setHearingDate(transcriptionEntity.getHearing().getHearingDate());
            }
            if (transcriptionEntity.getTranscriptionUrgency() != null) {
                transcriptionResponse.setUrgency(transcriptionEntity.getTranscriptionUrgency().getDescription());
            }
            transcriptionResponse.setRequestType(transcriptionEntity.getTranscriptionType().getDescription());
            transcriptionResponse.setTranscriptionStartTs(transcriptionEntity.getStartTime());
            transcriptionResponse.setTranscriptionEndTs(transcriptionEntity.getEndTime());
        } catch (Exception exception) {
            throw new DartsApiException(TranscriptionApiError.TRANSCRIPTION_NOT_FOUND);
        }
        return transcriptionResponse;

    }

    /*
    Returns the transcription comment that was added when the transcription was set to this status in the workflow.
     */
    private String getTranscriptionCommentAtStatus(TranscriptionEntity transcriptionEntity, TranscriptionStatusEnum status) {
        Optional<TranscriptionWorkflowEntity> foundWorkflowEntityOpt = transcriptionEntity.getTranscriptionWorkflowEntities().stream()
            .filter(workflow -> workflow.getTranscriptionStatus().getId().equals(status.getId())).findAny();
        if (foundWorkflowEntityOpt.isEmpty()) {
            return null;
        }
        List<TranscriptionCommentEntity> transcriptionCommentEntities = foundWorkflowEntityOpt.get().getTranscriptionComments();
        if (transcriptionCommentEntities.isEmpty()) {
            return null;
        }
        return StringUtils.trimToNull(transcriptionCommentEntities.get(0).getComment());

    }

    private String getRequestorName(TranscriptionEntity transcriptionEntity) {
        if (transcriptionEntity.getCreatedBy() != null) {
            return transcriptionEntity.getCreatedBy().getUsername();
        } else {
            return transcriptionEntity.getRequestor();
        }
    }
}
