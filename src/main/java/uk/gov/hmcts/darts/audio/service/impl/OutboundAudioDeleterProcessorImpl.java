package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessorSingleElement;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.MEDIA_IN_PERPETUITY;
import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.SUPER_USER;


@Service
@RequiredArgsConstructor
@Slf4j
public class OutboundAudioDeleterProcessorImpl implements OutboundAudioDeleterProcessor {
    private final UserAccountRepository userAccountRepository;
    private final LastAccessedDeletionDayCalculator deletionDayCalculator;
    private final UserIdentity userIdentity;
    private final TransformedMediaRepository transformedMediaRepository;
    private final OutboundAudioDeleterProcessorSingleElement singleElementProcessor;

    @Value("${darts.audio.outbounddeleter.last-accessed-deletion-day:2}")
    private int deletionDays;

    @Override
    public List<TransientObjectDirectoryEntity> markForDeletion(Integer batchSize) {

        List<TransientObjectDirectoryEntity> deletedValues = new ArrayList<>();

        UserAccountEntity systemUser = userIdentity.getUserAccount();

        OffsetDateTime deletionStartDateTime = deletionDayCalculator.getStartDateForDeletion(deletionDays);

        List<Integer> transformedMediaListIds = transformedMediaRepository.findAllDeletableTransformedMedia(
            deletionStartDateTime, Limit.of(batchSize));
        if (transformedMediaListIds.isEmpty()) {
            log.debug("No transformed media to be marked for deletion");
        } else {
            Set<MediaRequestEntity> mediaRequests = new HashSet<>();
            for (Integer transformedMediaId : transformedMediaListIds) {
                try {
                    Optional<TransformedMediaEntity> transformedMediaOpt = transformedMediaRepository.findById(transformedMediaId);
                    if (transformedMediaOpt.isEmpty()) {
                        log.error("TransformedMediaEntity with id {} not found", transformedMediaId);
                        continue;
                    }
                    TransformedMediaEntity transformedMedia = transformedMediaOpt.get();
                    if (isTransformedMediaEligibleForDelete(transformedMedia)) {
                        List<TransientObjectDirectoryEntity> deleted = singleElementProcessor.markForDeletion(systemUser, transformedMedia);
                        deletedValues.addAll(deleted);
                    }
                    mediaRequests.add(transformedMedia.getMediaRequest());
                } catch (Exception exception) {
                    log.error("Unable to mark for deletion transformed media {}", transformedMediaId, exception);
                }
            }
            mediaRequests.forEach(mr -> singleElementProcessor.markMediaRequestAsExpired(mr, systemUser));
        }

        return deletedValues;
    }

    private boolean isTransformedMediaEligibleForDelete(TransformedMediaEntity transformedMedia) {
        return !transformedMedia.isOwnerInSecurityGroup(List.of(MEDIA_IN_PERPETUITY, SUPER_ADMIN, SUPER_USER));
    }

    @Override
    public void setDeletionDays(int deletionDays) {
        this.deletionDays = deletionDays;
    }
}