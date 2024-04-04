package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExternalObjectDirectoryServiceImpl implements ExternalObjectDirectoryService {

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
	
    @Transactional
    public Optional<ExternalObjectDirectoryEntity> eagerLoadExternalObjectDirectory(Integer externalObjectDirectoryId) {
        return eodRepository.findById(externalObjectDirectoryId);
    }

    @Transactional
    public void updateStatus(ObjectRecordStatusEntity newStatus, UserAccountEntity userAccount, List<Integer> idsToUpdate, OffsetDateTime timestamp) {
        eodRepository.updateStatus(newStatus, userAccount, idsToUpdate, timestamp);
    }

}
