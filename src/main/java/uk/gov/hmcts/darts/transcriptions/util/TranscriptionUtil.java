package uk.gov.hmcts.darts.transcriptions.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.util.List;
import java.util.Optional;

@UtilityClass
public class TranscriptionUtil {


    /*
    Returns the transcription comment that was added when the transcription was set to this status in the workflow.
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
        return StringUtils.trimToNull(transcriptionCommentEntities.get(0).getComment());
    }
}
