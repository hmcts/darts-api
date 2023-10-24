package uk.gov.hmcts.darts.common.mapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.transcriptions.model.Transcript;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class TranscriptionMapper {

    public List<Transcript> mapResponse(List<TranscriptionEntity> transcriptionEntities) {
        List<Transcript> response = new ArrayList<>();
        for (TranscriptionEntity transcriptionEntity : transcriptionEntities) {
            response.add(map(transcriptionEntity));
        }
        return response;
    }

    private Transcript map(TranscriptionEntity transcriptionEntity) {
        Transcript transcript = new Transcript();
        transcript.setTraId(transcriptionEntity.getId());
        HearingEntity hearing = transcriptionEntity.getHearing();
        if (hearing == null) {
            if (transcriptionEntity.getHearingDate() != null) {
                transcript.setHearingDate(transcriptionEntity.getHearingDate().toLocalDate());
            }
        } else {
            transcript.setHeaId(hearing.getId());
            transcript.setHearingDate(hearing.getHearingDate());
        }
        transcript.setType(transcriptionEntity.getTranscriptionType().getDescription());
        transcript.setRequestedOn(transcriptionEntity.getCreatedDateTime().toLocalDate());
        transcript.setRequestedByName(getRequestedBy(transcriptionEntity));
        transcript.setStatus(transcriptionEntity.getTranscriptionStatus().getStatusType());
        return transcript;
    }

    public List<Transcript> mapHearingResponse(List<TranscriptionEntity> transcriptionEntities) {
        List<Transcript> response = new ArrayList<>();
        for (TranscriptionEntity transcriptionEntity : transcriptionEntities) {
            response.add(mapHearing(transcriptionEntity));
        }
        return response;
    }

    private Transcript mapHearing(TranscriptionEntity transcriptionEntity) {
        Transcript transcript = new Transcript();
        transcript.setTraId(transcriptionEntity.getId());
        HearingEntity hearing = transcriptionEntity.getHearing();
        if (hearing == null) {
            if (transcriptionEntity.getHearingDate() != null) {
                transcript.setHearingDate(transcriptionEntity.getHearingDate().toLocalDate());
            }
        } else {
            transcript.setHeaId(hearing.getId());
            transcript.setHearingDate(hearing.getHearingDate());
        }
        transcript.setType(transcriptionEntity.getTranscriptionType().getDescription());
        transcript.setRequestedOn(transcriptionEntity.getCreatedDateTime().toLocalDate());
        transcript.setRequestedByName(getRequestedBy(transcriptionEntity));
        transcript.setStatus(transcriptionEntity.getTranscriptionStatus().getStatusType());
        return transcript;
    }

    private String getRequestedBy(TranscriptionEntity transcriptionEntity) {
        if (transcriptionEntity.getCreatedBy() != null) {
            return transcriptionEntity.getCreatedBy().getUsername();
        } else {
            return transcriptionEntity.getRequestor();
        }
    }
}
