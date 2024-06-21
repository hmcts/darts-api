package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExternalObjectDirectoryServiceImpl implements ExternalObjectDirectoryService {

    private static final int INITIAL_VERIFICATION_ATTEMPTS = 1;

    private final ExternalObjectDirectoryRepository eodRepository;
    private final ArmDataManagementConfiguration armConfig;

    @Override
    public List<ExternalObjectDirectoryEntity> findFailedStillRetriableArmEods(Pageable pageable) {

        return eodRepository.findNotFinishedAndNotExceededRetryInStorageLocation(
            EodHelper.failedArmStatuses(),
            EodHelper.armLocation(),
            armConfig.getMaxRetryAttempts(),
            pageable
        );
    }

    @Override
    public boolean hasAllMediaBeenCopiedFromInboundStorage(List<MediaEntity> mediaEntities) {
        return mediaEntities.stream().allMatch(this::hasMediaBeenCopiedFromInboundStorage);
    }

    private boolean hasMediaBeenCopiedFromInboundStorage(MediaEntity mediaEntity) {
        return !eodRepository.hasMediaNotBeenCopiedFromInboundStorage(
            mediaEntity,
            EodHelper.storedStatus(),
            EodHelper.inboundLocation(),
            EodHelper.awaitingVerificationStatus(),
            List.of(EodHelper.unstructuredLocation(), EodHelper.armLocation()));
    }

    @Override
    @Transactional
    public Optional<ExternalObjectDirectoryEntity> eagerLoadExternalObjectDirectory(Integer externalObjectDirectoryId) {
        return eodRepository.findById(externalObjectDirectoryId);
    }

    @Override
    @Transactional
    public void updateStatus(ObjectRecordStatusEntity newStatus, UserAccountEntity userAccount, List<Integer> idsToUpdate, OffsetDateTime timestamp) {
        eodRepository.updateStatus(newStatus, userAccount, idsToUpdate, timestamp);
    }

    @Override
    @Transactional
    public ExternalObjectDirectoryEntity createAndSaveExternalObjectDirectory(UUID externalLocation,
                                                                              UserAccountEntity userAccountEntity,
                                                                              CaseDocumentEntity caseDocumentEntity,
                                                                              ExternalLocationTypeEntity externalLocationType) {
        var externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setCaseDocument(caseDocumentEntity);
        externalObjectDirectoryEntity.setStatus(EodHelper.storedStatus());
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationType);
        externalObjectDirectoryEntity.setExternalLocation(externalLocation);
        externalObjectDirectoryEntity.setChecksum(caseDocumentEntity.getChecksum());
        externalObjectDirectoryEntity.setVerificationAttempts(INITIAL_VERIFICATION_ATTEMPTS);
        externalObjectDirectoryEntity.setCreatedBy(userAccountEntity);
        externalObjectDirectoryEntity.setLastModifiedBy(userAccountEntity);
        return eodRepository.save(externalObjectDirectoryEntity);
    }

}
