package uk.gov.hmcts.darts.usermanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.TranscriptionMapper;
import uk.gov.hmcts.darts.usermanagement.model.TranscriptionDetails;
import uk.gov.hmcts.darts.usermanagement.service.TranscriptionService;

import java.time.OffsetDateTime;
import java.util.List;

import static uk.gov.hmcts.darts.usermanagement.exception.UserManagementError.TRANSCRIPTION_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class TranscriptionServiceImpl implements TranscriptionService {

    private TranscriptionRepository repository;

    private TranscriptionMapper transcriptionMapper;

    @Override
    public TranscriptionDetails getTranscriptionsForUser(Integer userId, OffsetDateTime requestedAtFrom) {
        TranscriptionDetails details = new TranscriptionDetails();

        List<TranscriptionEntity> entityList = repository.findTranscriptionForUserOnOrAfterDate(userId, requestedAtFrom);

        if (entityList.isEmpty()) {
            throw new DartsApiException(TRANSCRIPTION_NOT_FOUND);
        }

        for (TranscriptionEntity transcriptionEntity : entityList) {
            details.addTransactionsItem(transcriptionMapper.mapTransactionEntityToTransactionDetails(transcriptionEntity));
        }

        return details;
    }
}