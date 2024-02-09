package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessorSingleElement;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


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

    public List<TransientObjectDirectoryEntity> markForDeletion() {

        UserAccountEntity systemUser = userAccountRepository.findSystemUser(systemUserHelper.findSystemUserGuid(
                "housekeeping"));
        if (systemUser == null) {
            throw new DartsApiException(AudioApiError.MISSING_SYSTEM_USER);
        }

        OffsetDateTime deletionStartDateTime = deletionDayCalculator.getStartDateForDeletion(deletionDays);

        List<TransformedMediaEntity> transformedMediaList = transformedMediaRepository.findAllDeletableTransformedMedia(
            deletionStartDateTime);

        List<TransientObjectDirectoryEntity> deletedValues = new ArrayList<>();
        for (TransformedMediaEntity transformedMedia: transformedMediaList) {
            try {
                deletedValues.addAll(singleElementProcessor.markForDeletion(systemUser, transformedMedia));
            } catch (Exception exception) {
                log.error("Unable to mark for deletion transformed media with id: {}", transformedMedia.getId(), exception);
            }
        }

        Set<MediaRequestEntity> mediaRequests = transformedMediaList.stream().map(TransformedMediaEntity::getMediaRequest).collect(Collectors.toSet());
        mediaRequests.forEach(mr -> singleElementProcessor.markMediaRequestAsExpired(mr, systemUser));

        return deletedValues;
    }

    @Override
    public void setDeletionDays(int deletionDays) {
        this.deletionDays = deletionDays;
    }
}
