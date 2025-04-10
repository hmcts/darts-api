package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.service.InboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryQueryTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InboundAudioDeleterProcessorImpl implements InboundAudioDeleterProcessor {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final UserIdentity userIdentity;
    private final EodHelper eodHelper;

    @Value("${darts.automated.task.inbound-audio-deleter.unstructured-minimum-duration}")
    Duration durationInUnstructured;

    private static final int MAX_IDS_PER_LOG_MESSAGE = 1000;

    @Override
    public void markForDeletion(int batchSize) {
        OffsetDateTime lastModifiedBefore = currentTimeHelper.currentOffsetDateTime().minus(durationInUnstructured);

        List<Integer> audioFileIdsToBeMarked = externalObjectDirectoryRepository.findIdsIn2StorageLocationsBeforeTime(
            EodHelper.storedStatus(),
            EodHelper.storedStatus(),
            EodHelper.inboundLocation(),
            EodHelper.unstructuredLocation(),
            lastModifiedBefore,
            ExternalObjectDirectoryQueryTypeEnum.MEDIA_QUERY.getIndex(),
            Limit.of(batchSize)
        );
        log.info("Marking {} inbound audio files to be deleted out of a batch size {}", audioFileIdsToBeMarked.size(), batchSize);

        if (audioFileIdsToBeMarked.isEmpty()) {
            log.debug("No Inbound Audio files found that need to be marked for deletion.");
            return;
        }

        logDeletion(audioFileIdsToBeMarked);

        UserAccountEntity user = userIdentity.getUserAccount();
        eodHelper.updateStatus(
            EodHelper.markForDeletionStatus(),
            user,
            audioFileIdsToBeMarked,
            OffsetDateTime.now()
        );
    }

    private void logDeletion(List<Integer> audioFileIdsToBeMarked) {
        //Azure will only log 32k characters, so want to balance between not too many messages in the logs and
        // allowing all the ids to fit without being truncated
        List<List<Integer>> splitList = ListUtils.partition(audioFileIdsToBeMarked,
                                                            MAX_IDS_PER_LOG_MESSAGE);
        for (List<Integer> eodIds : splitList) {
            log.info("Marking EODs to be marked for deletion with ids : {}", eodIds);
        }
    }
}