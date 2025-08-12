package uk.gov.hmcts.darts.audio.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessorSingleElement;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
//NOTE: When this class is edited it is important we manually test the outbound audio deletion process
//This is because lazy loading errors are not be detected in integration tests
public class OutboundAudioDeleterProcessorImpl implements OutboundAudioDeleterProcessor {
    private final UserAccountRepository userAccountRepository;
    private final LastAccessedDeletionDayCalculator deletionDayCalculator;
    private final TransformedMediaRepository transformedMediaRepository;
    private final OutboundAudioDeleterProcessorSingleElement singleElementProcessor;
    private final TransformedMediaEntityProcessor transformedMediaEntityProcessor;

    @Value("${darts.audio.outbounddeleter.last-accessed-deletion-day:2}")
    @Getter
    private int deletionDays;

    @Override
    public List<TransientObjectDirectoryEntity> markForDeletion(Integer batchSize) {

        List<TransientObjectDirectoryEntity> deletedValues = new ArrayList<>();

        OffsetDateTime deletionStartDateTime = deletionDayCalculator.getStartDateForDeletion(getDeletionDays());

        List<Integer> transformedMediaListIds = transformedMediaRepository.findAllDeletableTransformedMedia(
            deletionStartDateTime,
            ObjectRecordStatusEnum.getExpiredStatusIds(),
            Limit.of(batchSize));

        if (transformedMediaListIds.isEmpty()) {
            log.debug("No transformed media to be marked for deletion");
        } else {
            log.info("Found {} transformed media to be marked for deletion out of batch size {}", transformedMediaListIds.size(), batchSize);
            Set<Integer> mediaRequestIds = new HashSet<>();
            for (Integer transformedMediaId : transformedMediaListIds) {
                Integer mediaRequestId = transformedMediaEntityProcessor.process(transformedMediaId, deletedValues);
                if (mediaRequestId != null) {
                    mediaRequestIds.add(mediaRequestId);
                }
            }
            mediaRequestIds.forEach(singleElementProcessor::markMediaRequestAsExpired);
        }
        return deletedValues;
    }

    @Service
    @RequiredArgsConstructor
    public static class TransformedMediaEntityProcessor {
        private final OutboundAudioDeleterProcessorSingleElement singleElementProcessor;
        private final TransformedMediaRepository transformedMediaRepository;


        @Transactional
        public Integer process(Integer transformedMediaId, List<TransientObjectDirectoryEntity> deletedValues) {
            try {
                log.info("Processing transformed media {}", transformedMediaId);
                Optional<TransformedMediaEntity> transformedMediaOpt = transformedMediaRepository.findById(transformedMediaId);
                if (transformedMediaOpt.isEmpty()) {
                    log.error("TransformedMediaEntity with id {} not found", transformedMediaId);
                    return null;
                }
                TransformedMediaEntity transformedMedia = transformedMediaOpt.get();
                deletedValues.addAll(singleElementProcessor.markForDeletion(transformedMedia));
                return transformedMedia.getMediaRequest().getId();
            } catch (Exception exception) {
                log.error("Unable to mark for deletion transformed media {}", transformedMediaId, exception);
            }
            return null;
        }
    }
}