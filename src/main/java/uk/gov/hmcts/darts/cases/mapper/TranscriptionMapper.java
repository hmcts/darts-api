package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.cases.model.Transcript;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;

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
        transcript.setTranscriptionId(transcriptionEntity.getId());
        List<HearingEntity> hearings = transcriptionEntity.getHearings();
        if (hearings.isEmpty()) {
            if (transcriptionEntity.getHearingDate() != null) {
                transcript.setHearingDate(transcriptionEntity.getHearingDate());
            }
        } else {
            HearingEntity hearing = hearings.get(0);
            transcript.setHearingId(hearing.getId());
            transcript.setHearingDate(hearing.getHearingDate());
        }
        transcript.setType(transcriptionEntity.getTranscriptionType().getDescription());
        transcript.setRequestedOn(transcriptionEntity.getCreatedDateTime());
        transcript.setRequestedByName(getRequestedBy(transcriptionEntity));
        transcript.setStatus(transcriptionEntity.getTranscriptionStatus().getStatusType());
        return transcript;
    }

    private String getRequestedBy(TranscriptionEntity transcriptionEntity) {
        if (transcriptionEntity.getCreatedBy() != null) {
            return transcriptionEntity.getCreatedBy().getUserName();
        } else {
            return transcriptionEntity.getRequestor();
        }
    }


}
