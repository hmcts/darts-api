package uk.gov.hmcts.darts.arm.component.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.component.ArmRetentionEventDateCalculator;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

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

    private UserAccountEntity userAccount;

    @Transactional
    public void calculateRetentionEventDate(Integer externalObjectDirectoryId) {
        userAccount = userIdentity.getUserAccount();
        try {
            ExternalObjectDirectoryEntity externalObjectDirectory = externalObjectDirectoryRepository.findById(externalObjectDirectoryId).orElseThrow();
            OffsetDateTime retentionDate = getDocumentRetentionDate(externalObjectDirectory);
            if (nonNull(retentionDate)) {
                OffsetDateTime armRetentionDate = retentionDate.minusDays(100);
                if (armRetentionDate.truncatedTo(MILLIS).compareTo(externalObjectDirectory.getEventDateTs().truncatedTo(MILLIS)) != 0) {
                    log.debug("Updating retention date for ARM EOD {} ", externalObjectDirectoryId);
                    UpdateMetadataResponse updateMetadataResponse = armDataManagementApi.updateMetadata(
                        String.valueOf(externalObjectDirectoryId), armRetentionDate);
                    if (!updateMetadataResponse.isError()) {
                        externalObjectDirectory.setEventDateTs(armRetentionDate);
                        externalObjectDirectory.setUpdateRetention(false);
                        externalObjectDirectory.setLastModifiedBy(userAccount);
                        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
                    } else {
                        log.error("Unable set retention date for ARM EOD {} due to error(s) {}",
                                  externalObjectDirectoryId, StringUtils.join(updateMetadataResponse.getResponseStatusMessages(), ", "));
                    }
                } else {
                    externalObjectDirectory.setUpdateRetention(false);
                    externalObjectDirectory.setLastModifiedBy(userAccount);
                    externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
                }
            } else {
                log.warn("Retention date has not be set for EOD {}", externalObjectDirectoryId);
            }
        } catch (Exception e) {
            log.error("Unable to calculate ARM retention date for EOD {}", externalObjectDirectoryId, e);
        }
    }

    private OffsetDateTime getDocumentRetentionDate(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        OffsetDateTime retentionDate = null;
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
