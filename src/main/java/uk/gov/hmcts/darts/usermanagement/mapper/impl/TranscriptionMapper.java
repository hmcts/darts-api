package uk.gov.hmcts.darts.usermanagement.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.usermanagement.model.TranscriptionDetail;
import uk.gov.hmcts.darts.usermanagement.model.TranscriptionDetails;

@Component
@RequiredArgsConstructor
public class TranscriptionMapper {

    public TranscriptionDetail mapTransactionEntityToTransactionDetails(TranscriptionEntity transcriptionEntity) {
        TranscriptionDetail details = new TranscriptionDetail();
        details.setTranscriptionStatusId(transcriptionEntity.getTranscriptionStatus().getId());
        details.setTranscriptionId(transcriptionEntity.getId());
        details.setCaseNumber(transcriptionEntity.getCourtCase().getCaseNumber());
        details.setCourthouseId(transcriptionEntity.getCourtCase().getCourthouse().getId());
        details.setHearingDate(transcriptionEntity.getHearingDate());
        details.requestedAt(transcriptionEntity.getCreatedDateTime());
        return details;
    }
}