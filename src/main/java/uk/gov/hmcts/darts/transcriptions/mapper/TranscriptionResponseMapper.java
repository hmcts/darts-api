package uk.gov.hmcts.darts.transcriptions.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.audio.exception.*;
import uk.gov.hmcts.darts.cases.model.*;
import uk.gov.hmcts.darts.common.entity.*;
import uk.gov.hmcts.darts.common.exception.*;
import uk.gov.hmcts.darts.transcriptions.exception.*;
import uk.gov.hmcts.darts.transcriptions.model.*;

import java.util.List;
import java.util.stream.Collectors;

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
            transcriptionResponse.setCaseId(Integer.valueOf(transcriptionEntity.getCourtCase().getId()));
            transcriptionResponse.setCaseNumber(transcriptionEntity.getCourtCase().getCaseNumber());
            transcriptionResponse.setCourthouse(transcriptionEntity.getCourtCase().getCourthouse().getCourthouseName());
            transcriptionResponse.setDefendants(transcriptionEntity.getCourtCase().getDefendantStringList());
            transcriptionResponse.setJudges(transcriptionEntity.getCourtCase().getJudgeStringList());
            transcriptionResponse.setTranscriptFileName(transcriptionEntity.getTranscriptionDocument().getFileName());
            transcriptionResponse.setHearingDate(transcriptionEntity.getHearing().getHearingDate());
            transcriptionResponse.setUrgency(transcriptionEntity.getTranscriptionUrgency().getDescription());
            transcriptionResponse.setRequestType(transcriptionEntity.getTranscriptionType().getDescription());
            transcriptionResponse.setTranscriptionStartTs(String.valueOf(transcriptionEntity.getStartTime()));
            transcriptionResponse.setTranscriptionEndTs(String.valueOf(transcriptionEntity.getEndTime()));
        }
        catch(Exception exception) {
            throw new DartsApiException(TranscriptionApiError.INTERNAL_SERVER_ERROR);
        }
        return transcriptionResponse;

    }
}
