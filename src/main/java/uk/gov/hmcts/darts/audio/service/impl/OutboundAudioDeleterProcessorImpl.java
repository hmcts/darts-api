package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessorSingleElement;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.MEDIA_IN_PERPETUITY;
import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.SUPER_USER;


@Service
@RequiredArgsConstructor
@Slf4j
public class OutboundAudioDeleterProcessorImpl implements OutboundAudioDeleterProcessor {
    private final UserAccountRepository userAccountRepository;
    private final LastAccessedDeletionDayCalculator deletionDayCalculator;
    private final SystemUserHelper systemUserHelper;
    private final TransformedMediaRepository transformedMediaRepository;
    private final OutboundAudioDeleterProcessorSingleElement singleElementProcessor;

    @Value("${darts.audio.outbounddeleter.last-accessed-deletion-day:2}")
    private int deletionDays;

    @Override
    public List<TransientObjectDirectoryEntity> markForDeletion(Integer batchSize) {

        List<TransientObjectDirectoryEntity> deletedValues = new ArrayList<>();

        UserAccountEntity systemUser = systemUserHelper.getReferenceTo(SystemUsersEnum.OUTBOUND_AUDIO_DELETER_AUTOMATED_TASK);

        OffsetDateTime deletionStartDateTime = deletionDayCalculator.getStartDateForDeletion(deletionDays);

        List<TransformedMediaEntity> transformedMediaList = transformedMediaRepository.findAllDeletableTransformedMedia(
            deletionStartDateTime, Limit.of(batchSize));
        if (transformedMediaList.isEmpty()) {
            log.debug("No transformed media to be marked for deletion");
        } else {
            for (TransformedMediaEntity transformedMedia : transformedMediaList) {
                try {
                    if (isTransformedMediaEligibleForDelete(transformedMedia)) {
                        List<TransientObjectDirectoryEntity> deleted = singleElementProcessor.markForDeletion(systemUser, transformedMedia);
                        deletedValues.addAll(deleted);
                    }
                } catch (Exception exception) {
                    log.error("Unable to mark for deletion transformed media {}", transformedMedia.getId(), exception);
                }
            }
            Set<MediaRequestEntity> mediaRequests = transformedMediaList.stream().map(TransformedMediaEntity::getMediaRequest).collect(toSet());
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