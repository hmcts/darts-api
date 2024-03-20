package uk.gov.hmcts.darts.arm.service.impl;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Component
@Getter
@Accessors(fluent = true)
public class EodEntities {

    private final ExternalObjectDirectoryRepository eodRepository;
    private final ExternalLocationTypeRepository eltRepository;
    private final ObjectRecordStatusRepository orsRepository;

    public static ExternalLocationTypeEntity unstructuredLocation;
    public static ExternalLocationTypeEntity armLocation;
    public static ExternalLocationTypeEntity detsLocation;

    public static ObjectRecordStatusEntity failedArmRawDataStatus;
    public static ObjectRecordStatusEntity failedArmManifestFileStatus;
    public static ObjectRecordStatusEntity failedArmResponseManifestFileStatus;
    public static ObjectRecordStatusEntity storedStatus;
    public static ObjectRecordStatusEntity armIngestionStatus;
    public static ObjectRecordStatusEntity armDropZoneStatus;
    public static List<ObjectRecordStatusEntity> failedArmStatuses;

    public EodEntities(ExternalObjectDirectoryRepository eodRepository,
                       ExternalLocationTypeRepository eltRepository,
                       ObjectRecordStatusRepository orsRepository) {
        this.eodRepository = eodRepository;
        this.eltRepository = eltRepository;
        this.orsRepository = orsRepository;

        unstructuredLocation = eltRepository.getReferenceById(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        armLocation = eltRepository.getReferenceById(ExternalLocationTypeEnum.ARM.getId());
        detsLocation = eltRepository.getReferenceById(ExternalLocationTypeEnum.DETS.getId());

        //TODO make these getReference?
        storedStatus = orsRepository.findById(STORED.getId()).get();
        failedArmRawDataStatus = orsRepository.findById(ARM_RAW_DATA_FAILED.getId()).get();
        failedArmManifestFileStatus = orsRepository.findById(ARM_MANIFEST_FAILED.getId()).get();
        failedArmResponseManifestFileStatus = orsRepository.findById(ARM_RESPONSE_MANIFEST_FAILED.getId()).get();
        armIngestionStatus = orsRepository.findById(ARM_INGESTION.getId()).get();
        armDropZoneStatus = orsRepository.findById(ARM_DROP_ZONE.getId()).get();

        failedArmStatuses  = List.of(failedArmRawDataStatus, failedArmManifestFileStatus);
    }

    public static boolean isEqual(ObjectRecordStatusEntity ors1, ObjectRecordStatusEntity ors2) {
        return ors1.getId().equals(ors2.getId());
    }

    public static boolean equalsAny(ObjectRecordStatusEntity ors, ObjectRecordStatusEntity... orsEntitiesToCompareTo) {
        return Arrays.stream(orsEntitiesToCompareTo).map(ObjectRecordStatusEntity::getId).anyMatch(orsToCompareTo -> orsToCompareTo.equals(ors.getId()));
    }
}
