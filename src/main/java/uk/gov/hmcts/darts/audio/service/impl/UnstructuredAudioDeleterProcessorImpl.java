package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.service.UnstructuredAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
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

    @Value("${darts.automated-task.unstructured-audio-deleter.minimum-duration-in-unstructured}")
    Duration durationInUnstructured;

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final UserAccountRepository userAccountRepository;
    private final SystemUserHelper systemUserHelper;

    private final EodHelper eodHelper;

    @Override
    public void markForDeletion() {

        OffsetDateTime unstructuredModifiedBeforeDateTime = currentTimeHelper.currentOffsetDateTime().minusSeconds(durationInUnstructured.getSeconds());

        List<Integer> audioFileIdsToBeMarked = externalObjectDirectoryRepository.findIdsForAudioToBeDeletedFromUnstructured(
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            EodHelper.armLocation(),
            unstructuredModifiedBeforeDateTime
        );

        if (audioFileIdsToBeMarked.isEmpty()) {
            log.debug("No Unstructured Audio files found that need to be marked for deletion.");
            return;
        }
        log.debug("Marking the following Unstructured ExternalObjectDirectory.Id's for deletion:- {}", audioFileIdsToBeMarked);

        UserAccountEntity user = userAccountRepository.findSystemUser(systemUserHelper.findSystemUserGuid("housekeeping"));

        eodHelper.updateStatus(
            EodHelper.markForDeletionStatus(),
            user,
            audioFileIdsToBeMarked,
            OffsetDateTime.now()
        );
        audioFileIdsToBeMarked.stream().forEach(eodId -> log.info("Set status of EOD {} to be marked for deletion", eodId));

    }
}