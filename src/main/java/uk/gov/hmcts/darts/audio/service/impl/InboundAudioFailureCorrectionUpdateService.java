package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.util.List;

@Service
@RequiredArgsConstructor
class InboundAudioFailureCorrectionUpdateService {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Transactional
    public void markEodAsStored(Long eodId, Integer userId) {
        externalObjectDirectoryRepository.updateEodStatusAndTransferAttemptsWhereIdIn(
            EodHelper.storedStatus(), 0, userId, List.of(eodId)
        );
    }
}

