package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalObjectDirectoryServiceImpl implements ExternalObjectDirectoryService {

    private final ExternalObjectDirectoryRepository eodRepository;
    private final ArmDataManagementConfiguration armConfig;

    public List<ExternalObjectDirectoryEntity> findFailedStillRetriableArmEODs(Pageable pageable) {

        return eodRepository.findNotFinishedAndNotExceededRetryInStorageLocation(
            EodEntities.failedArmStatuses,
            EodEntities.armLocation,
            armConfig.getMaxRetryAttempts(),
            pageable
        );
    }

}
