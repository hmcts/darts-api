package uk.gov.hmcts.darts.transcriptions.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;

import java.util.List;
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

    public static TranscriptionResponse mapToTranscriptionResponse(TranscriptionEntity transcriptionEntity) {

        TranscriptionResponse transcriptionResponse = new TranscriptionResponse();
        try {
            CourtCaseEntity courtCase = transcriptionEntity.getCourtCase();
            transcriptionResponse.setCaseId(courtCase.getId());
            transcriptionResponse.setCaseNumber(courtCase.getCaseNumber());
            transcriptionResponse.setCourthouse(courtCase.getCourthouse().getCourthouseName());
            transcriptionResponse.setDefendants(courtCase.getDefendantStringList());
            transcriptionResponse.setJudges(courtCase.getJudgeStringList());
            transcriptionResponse.setFrom(transcriptionEntity.getCreatedBy().getUsername());

            if (transcriptionEntity.getTranscriptionStatus() != null) {
                transcriptionResponse.setStatus(transcriptionEntity.getTranscriptionStatus().getStatusType());
            }

            transcriptionResponse.setReceived(transcriptionEntity.getCreatedDateTime());

            // add comments mapped to the workflow that is also mapped to the transaction
            List<String> comments = transcriptionEntity.getTranscriptionWorkflowEntities().stream()
                .map(twe -> twe.getTranscriptionCommentEntities().stream()
                    .map(TranscriptionCommentEntity::getComment))
                .flatMap(list -> list.toList().stream()).collect(Collectors.toList());

            // add comments mapped to transaction
            comments.addAll(transcriptionEntity.getTranscriptionCommentEntities().stream().map(
                TranscriptionCommentEntity::getComment).toList());
            transcriptionResponse.setComments(comments);

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
}
