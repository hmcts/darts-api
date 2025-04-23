package uk.gov.hmcts.darts.transcriptions.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@UtilityClass
public class TranscriptionUtil {

    /**
     * Returns the transcription comment that was added when the transcription was set to this status in the workflow.
     */
    public String getTranscriptionCommentAtStatus(TranscriptionEntity transcriptionEntity, TranscriptionStatusEnum status) {
        Optional<TranscriptionWorkflowEntity> foundWorkflowEntityOpt = transcriptionEntity.getTranscriptionWorkflowEntities().stream()
            .filter(workflow -> workflow.getTranscriptionStatus().getId().equals(status.getId())).findAny();
        if (foundWorkflowEntityOpt.isEmpty()) {
            return null;
        }
        List<TranscriptionCommentEntity> transcriptionCommentEntities = foundWorkflowEntityOpt.get().getTranscriptionComments();
        if (transcriptionCommentEntities.isEmpty()) {
            return null;
        }
        return StringUtils.trimToNull(transcriptionCommentEntities.getFirst().getComment());
    }

    public OffsetDateTime getDateToLimitResults(Duration dateLimit) {
        return OffsetDateTime.now().minus(Period.ofDays((int) dateLimit.toDays()));
    }

    public static String getRequestedByName(TranscriptionEntity transcriptionEntity) {
        return Optional.ofNullable(transcriptionEntity.getRequestedBy())
            .map(UserAccountEntity::getUserFullName)
            .orElse(null);
    }

    public static Integer getRequestedById(TranscriptionEntity transcriptionEntity) {
        return Optional.ofNullable(transcriptionEntity.getRequestedBy())
            .map(UserAccountEntity::getId)
            .orElse(null);
    }
}
