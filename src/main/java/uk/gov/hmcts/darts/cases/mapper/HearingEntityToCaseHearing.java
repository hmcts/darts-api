package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class HearingEntityToCaseHearing {

    public List<Hearing> mapToHearingList(List<HearingEntity> hearingEntities, TranscriptionDocumentRepository transcriptionDocumentRepository) {

        List<Hearing> hearings = new ArrayList<>();

        if (!hearingEntities.isEmpty()) {

            for (HearingEntity entity : hearingEntities) {
                hearings.add(mapToHearing(entity, transcriptionDocumentRepository));
            }

        }

        return hearings;
    }

    private Hearing mapToHearing(HearingEntity entity, TranscriptionDocumentRepository transcriptionDocumentRepository) {

        Hearing hearing = new Hearing();

        hearing.setId(entity.getId());
        hearing.setDate(entity.getHearingDate());
        hearing.setJudges(entity.getJudgesStringList());
        hearing.setCourtroom(entity.getCourtroom().getName());
        var transcripts = entity.getTranscriptions()
            .stream()
            .filter(transcriptionEntity -> BooleanUtils.isTrue(transcriptionEntity.getIsManualTranscription())
                || StringUtils.isNotBlank(transcriptionEntity.getLegacyObjectId())
            )
            .filter(transcriptionEntity ->
                transcriptionDocumentRepository.findByTranscriptionIdAndHiddenTrueIncludeDeleted(transcriptionEntity.getId()).isEmpty()
            )
            .toList();
        hearing.setTranscriptCount(transcripts.size());

        return hearing;
    }

}
