package uk.gov.hmcts.darts.arm.service.impl;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Service
@Getter
@Accessors(fluent = true)
public class ExternalObjectDirectoryService {

    private final ExternalObjectDirectoryRepository eodRepository;
    private final ExternalLocationTypeRepository eltRepository;
    private final ObjectRecordStatusRepository orsRepository;

    private final ExternalLocationTypeEntity unstructuredLocation;
    private final ExternalLocationTypeEntity armLocation;
    private final ExternalLocationTypeEntity detsLocation;

    private final ObjectRecordStatusEntity failedArmRawDataStatus;
    private final ObjectRecordStatusEntity failedArmManifestFileStatus;
    private final ObjectRecordStatusEntity storedStatus;
    private final ObjectRecordStatusEntity armIngestionStatus;
    private final ObjectRecordStatusEntity armDropZoneStatus;
    private final List<ObjectRecordStatusEntity> failedArmStatuses;

    private final ArmDataManagementConfiguration armConfig;


    public ExternalObjectDirectoryService(ExternalObjectDirectoryRepository eodRepository,
                                          ExternalLocationTypeRepository eltRepository,
                                          ObjectRecordStatusRepository orsRepository,
                                          ArmDataManagementConfiguration armConfig) {
        this.eodRepository = eodRepository;
        this.eltRepository = eltRepository;
        this.orsRepository = orsRepository;
        this.armConfig = armConfig;

        unstructuredLocation = eltRepository.getReferenceById(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        armLocation = eltRepository.getReferenceById(ExternalLocationTypeEnum.ARM.getId());
        detsLocation = eltRepository.getReferenceById(ExternalLocationTypeEnum.DETS.getId());

        //TODO make these getReference?
        storedStatus = orsRepository.findById(STORED.getId()).get();
        failedArmRawDataStatus = orsRepository.findById(ARM_RAW_DATA_FAILED.getId()).get();
        failedArmManifestFileStatus = orsRepository.findById(ARM_MANIFEST_FAILED.getId()).get();
        armIngestionStatus = orsRepository.findById(ARM_INGESTION.getId()).get();
        armDropZoneStatus = orsRepository.findById(ARM_DROP_ZONE.getId()).get();

        failedArmStatuses  = List.of(failedArmRawDataStatus, failedArmManifestFileStatus);
    }

    public List<ExternalObjectDirectoryEntity> findFailedStillRetriableArmEODs(Pageable pageable) {

        return eodRepository.findNotFinishedAndNotExceededRetryInStorageLocation(
            failedArmStatuses,
            armLocation,
            armConfig.getMaxRetryAttempts(),
            pageable
        );
    }
}
