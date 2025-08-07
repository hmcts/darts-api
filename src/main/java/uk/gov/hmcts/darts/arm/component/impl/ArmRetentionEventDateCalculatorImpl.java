package uk.gov.hmcts.darts.arm.component.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.component.ArmRetentionEventDateCalculator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.helper.ArmHelper;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ConfidenceAware;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;

import java.time.OffsetDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.nonNull;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArmRetentionEventDateCalculatorImpl implements ArmRetentionEventDateCalculator {
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ArmDataManagementApi armDataManagementApi;
    private final UserIdentity userIdentity;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ArmHelper armHelper;

    @Transactional
    @Override
    public boolean calculateRetentionEventDate(Long externalObjectDirectoryId) {
        UserAccountEntity userAccount = userIdentity.getUserAccount();
        try {
            ExternalObjectDirectoryEntity externalObjectDirectory = externalObjectDirectoryRepository.findById(externalObjectDirectoryId).orElseThrow();
            OffsetDateTime retentionDate = getDocumentRetentionDate(externalObjectDirectory);
            if (nonNull(retentionDate)) {
                OffsetDateTime armRetentionDate = retentionDate.minusYears(armDataManagementConfiguration.getEventDateAdjustmentYears());
                if (nonNull(externalObjectDirectory.getEventDateTs())
                    && armRetentionDate.truncatedTo(MILLIS).isEqual(externalObjectDirectory.getEventDateTs().truncatedTo(MILLIS))) {
                    log.info("Event date found and different when compared to ARM retention date, resetting update retention flag for {} ",
                             externalObjectDirectoryId);
                    externalObjectDirectory.setUpdateRetention(false);
                    externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
                    return true;
                } else if (ObjectRecordStatusEnum.STORED.getId() == externalObjectDirectory.getStatusId()) {
                    log.info("Updating retention date for ARM EOD {} ", externalObjectDirectoryId);
                    return processArmUpdate(externalObjectDirectory, armRetentionDate, externalObjectDirectoryId);
                } else {
                    log.info("EOD {} is not in STORED status, skipping ARM retention date update", externalObjectDirectoryId);
                }
            } else {
                log.warn("Retention date has not be set for EOD {}", externalObjectDirectoryId);
            }
        } catch (Exception e) {
            log.error("Unable to calculate ARM retention date for EOD {}", externalObjectDirectoryId, e);
        }
        return false;
    }

    private boolean processArmUpdate(ExternalObjectDirectoryEntity externalObjectDirectory, OffsetDateTime armRetentionDate, Long externalObjectDirectoryId) {
        ConfidenceAware confidenceAware = armHelper.getDocumentConfidence(externalObjectDirectory);

        if (confidenceAware != null) {
            RetentionConfidenceScoreEnum confidenceScore = confidenceAware.getRetConfScore();
            String confidenceReason = confidenceAware.getRetConfReason();

            if (confidenceScore != null) {
                UpdateMetadataResponse updateMetadataResponseMedia = armDataManagementApi.updateMetadata(
                    externalObjectDirectory.getExternalRecordId(), armRetentionDate, confidenceScore, confidenceReason);

                if (updateMetadataResponseMedia.isError()) {
                    log.error("Unable to set retention date for ARM EOD {} due to error(s) {}",
                              externalObjectDirectoryId, StringUtils.join(updateMetadataResponseMedia.getResponseStatusMessages(), ", "));
                } else {
                    externalObjectDirectory.setEventDateTs(armRetentionDate);
                    externalObjectDirectory.setUpdateRetention(false);
                    externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
                    log.info("Retention date is successfully applied on ARM for EOD {} ", externalObjectDirectoryId);
                    return true;
                }
            } else {
                log.info("ARM event date calculation is skipped for EOD {} as no Retention Score is available", externalObjectDirectoryId);
            }
        }

        return false;
    }

    private OffsetDateTime getDocumentRetentionDate(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        OffsetDateTime retentionDate;
        if (nonNull(externalObjectDirectoryEntity.getMedia())) {
            retentionDate = externalObjectDirectoryEntity.getMedia().getRetainUntilTs();
        } else if (nonNull(externalObjectDirectoryEntity.getTranscriptionDocumentEntity())) {
            retentionDate = externalObjectDirectoryEntity.getTranscriptionDocumentEntity().getRetainUntilTs();
        } else if (nonNull(externalObjectDirectoryEntity.getAnnotationDocumentEntity())) {
            retentionDate = externalObjectDirectoryEntity.getAnnotationDocumentEntity().getRetainUntilTs();
        } else if (nonNull(externalObjectDirectoryEntity.getCaseDocument())) {
            retentionDate = externalObjectDirectoryEntity.getCaseDocument().getRetainUntilTs();
        } else {
            throw new DartsException("Unable to get object for EOD " + externalObjectDirectoryEntity.getId());
        }
        return retentionDate;
    }
}