package uk.gov.hmcts.darts.common.util;

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

    @Getter
    private static ExternalLocationTypeEntity unstructuredLocation;
    @Getter
    private static ExternalLocationTypeEntity armLocation;
    @Getter
    private static ExternalLocationTypeEntity detsLocation;

    @Getter
    private static ObjectRecordStatusEntity failedArmRawDataStatus;
    @Getter
    private static ObjectRecordStatusEntity failedArmManifestFileStatus;
    @Getter
    private static ObjectRecordStatusEntity storedStatus;
    @Getter
    private static ObjectRecordStatusEntity armIngestionStatus;
    @Getter
    private static ObjectRecordStatusEntity failedArmResponseManifestFileStatus;
    @Getter
    private static ObjectRecordStatusEntity armDropZoneStatus;
    @Getter
    private static List<ObjectRecordStatusEntity> failedArmStatuses;

    public EodEntities(ExternalObjectDirectoryRepository eodRepository,
                       ExternalLocationTypeRepository eltRepository,
                       ObjectRecordStatusRepository orsRepository) {
        this.eodRepository = eodRepository;
        this.eltRepository = eltRepository;
        this.orsRepository = orsRepository;

        unstructuredLocation = eltRepository.findById(ExternalLocationTypeEnum.UNSTRUCTURED.getId()).get();
        armLocation = eltRepository.findById(ExternalLocationTypeEnum.ARM.getId()).get();
        detsLocation = eltRepository.findById(ExternalLocationTypeEnum.DETS.getId()).get();

        storedStatus = orsRepository.findById(STORED.getId()).get();
        failedArmRawDataStatus = orsRepository.findById(ARM_RAW_DATA_FAILED.getId()).get();
        failedArmManifestFileStatus = orsRepository.findById(ARM_MANIFEST_FAILED.getId()).get();
        failedArmResponseManifestFileStatus = orsRepository.findById(ARM_RESPONSE_MANIFEST_FAILED.getId()).get();
        armIngestionStatus = orsRepository.findById(ARM_INGESTION.getId()).get();
        armDropZoneStatus = orsRepository.findById(ARM_DROP_ZONE.getId()).get();

        failedArmStatuses  = List.of(failedArmRawDataStatus, failedArmManifestFileStatus, failedArmResponseManifestFileStatus);
    }

    public static boolean isEqual(ObjectRecordStatusEntity ors1, ObjectRecordStatusEntity ors2) {
        return ors1.getId().equals(ors2.getId());
    }
    public static boolean isEqual(ExternalLocationTypeEntity olt1, ExternalLocationTypeEntity olt2) {
        return olt1.getId().equals(olt2.getId());
    }

    public static boolean equalsAny(ObjectRecordStatusEntity ors, ObjectRecordStatusEntity... orsEntitiesToCompareTo) {
        return Arrays.stream(orsEntitiesToCompareTo).map(ObjectRecordStatusEntity::getId).anyMatch(orsToCompareTo -> orsToCompareTo.equals(ors.getId()));
    }

}