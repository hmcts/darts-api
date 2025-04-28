package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.service.UnstructuredAudioDeleterProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnstructuredAudioDeleterProcessorImpl implements UnstructuredAudioDeleterProcessor {

    @Value("${darts.automated.task.unstructured-audio-deleter.minimum-duration-in-unstructured}")
    Duration durationInUnstructured;

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final UserAccountRepository userAccountRepository;
    private final UserIdentity userIdentity;

    private final EodHelper eodHelper;

    @Override
    public void markForDeletion(Integer batchSize) {

        OffsetDateTime unstructuredModifiedBeforeDateTime = currentTimeHelper.currentOffsetDateTime().minus(durationInUnstructured);

        List<Long> audioFileIdsToBeMarked = externalObjectDirectoryRepository.findIdsForAudioToBeDeletedFromUnstructured(
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            EodHelper.armLocation(),
            unstructuredModifiedBeforeDateTime,
            Limit.of(batchSize)
        );

        if (audioFileIdsToBeMarked.isEmpty()) {
            log.debug("No Unstructured Audio files found that need to be marked for deletion.");
            return;
        }
        log.info("Marking the following Unstructured ExternalObjectDirectory.Id's for deletion:- '{}' for batch size {}",
                 audioFileIdsToBeMarked, batchSize);

        UserAccountEntity user = userIdentity.getUserAccount();

        eodHelper.updateStatus(
            EodHelper.markForDeletionStatus(),
            user,
            audioFileIdsToBeMarked,
            OffsetDateTime.now()
        );
        audioFileIdsToBeMarked.stream().forEach(eodId -> log.info("Set status of EOD {} to be marked for deletion", eodId));

    }
}