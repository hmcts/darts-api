package uk.gov.hmcts.darts.common.mapper;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.model.TranscriptModel;
import uk.gov.hmcts.darts.transcriptions.util.TranscriptionUtil;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class TranscriptionMapper<T extends TranscriptModel> {

    public List<T> mapResponse(List<TranscriptionEntity> transcriptionEntities) {
        List<T> response = new ArrayList<>();
        for (TranscriptionEntity transcriptionEntity : transcriptionEntities) {
            if (Boolean.TRUE.equals(transcriptionEntity.getIsCurrent())) {
                response.add(map(transcriptionEntity));
            }
        }
        return response;
    }

    private T map(TranscriptionEntity transcriptionEntity) {
        T transcript = createNewTranscription();
        transcript.setTranscriptionId(transcriptionEntity.getId());
        List<HearingEntity> hearings = transcriptionEntity.getHearings();
        if (hearings.isEmpty()) {
            if (transcriptionEntity.getHearingDate() != null) {
                transcript.setHearingDate(transcriptionEntity.getHearingDate());
            }
        } else {
            HearingEntity hearing = hearings.getFirst();
            transcript.setHearingId(hearing.getId());
            transcript.setHearingDate(hearing.getHearingDate());
        }
        transcript.setCourtroom(transcriptionEntity.getCourtroom().getName());
        transcript.setType(transcriptionEntity.getTranscriptionType().getDescription());
        transcript.setRequestedOn(transcriptionEntity.getCreatedDateTime());
        transcript.setRequestedByName(TranscriptionUtil.getRequestedByName(transcriptionEntity));
        transcript.setStatus(transcriptionEntity.getTranscriptionStatus().getStatusType());
        return transcript;
    }

    protected abstract T createNewTranscription();

}