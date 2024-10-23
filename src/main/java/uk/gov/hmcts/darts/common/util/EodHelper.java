package uk.gov.hmcts.darts.common.util;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.AWAITING_VERIFICATION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.MARKED_FOR_DELETION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Component
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class EodHelper {

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
    private static ExternalLocationTypeEntity inboundLocation;

    @Getter
    private static ObjectRecordStatusEntity armIngestionStatus;
    @Getter
    private static ObjectRecordStatusEntity armDropZoneStatus;
    @Getter
    private static ObjectRecordStatusEntity armProcessingResponseFilesStatus;
    @Getter
    private static ObjectRecordStatusEntity failedArmRawDataStatus;
    @Getter
    private static ObjectRecordStatusEntity failedArmManifestFileStatus;
    @Getter
    private static ObjectRecordStatusEntity failedArmResponseManifestFileStatus;
    @Getter
    private static ObjectRecordStatusEntity storedStatus;
    @Getter
    private static ObjectRecordStatusEntity failureStatus;
    @Getter
    private static ObjectRecordStatusEntity markForDeletionStatus;
    @Getter
    private static ObjectRecordStatusEntity armResponseProcessingFailedStatus;
    @Getter
    private static ObjectRecordStatusEntity armResponseManifestFailedStatus;
    @Getter
    private static ObjectRecordStatusEntity armResponseChecksumVerificationFailedStatus;
    @Getter
    private static ObjectRecordStatusEntity armRpoPendingStatus;
    @Getter
    private static ObjectRecordStatusEntity awaitingVerificationStatus;

    @Getter
    private static List<ObjectRecordStatusEntity> failedArmStatuses;


    @SuppressWarnings("java:S2696")
    @PostConstruct
    public void init() {
        unstructuredLocation = eltRepository.findById(ExternalLocationTypeEnum.UNSTRUCTURED.getId()).orElseThrow();
        armLocation = eltRepository.findById(ExternalLocationTypeEnum.ARM.getId()).orElseThrow();
        detsLocation = eltRepository.findById(ExternalLocationTypeEnum.DETS.getId()).orElseThrow();
        inboundLocation = eltRepository.findById(ExternalLocationTypeEnum.INBOUND.getId()).orElseThrow();

        storedStatus = orsRepository.findById(STORED.getId()).orElseThrow();
        failureStatus = orsRepository.findById(FAILURE.getId()).orElseThrow();
        markForDeletionStatus = orsRepository.findById(MARKED_FOR_DELETION.getId()).orElseThrow();
        failedArmRawDataStatus = orsRepository.findById(ARM_RAW_DATA_FAILED.getId()).orElseThrow();
        armProcessingResponseFilesStatus = orsRepository.findById(ARM_PROCESSING_RESPONSE_FILES.getId()).orElseThrow();
        failedArmManifestFileStatus = orsRepository.findById(ARM_MANIFEST_FAILED.getId()).orElseThrow();
        failedArmResponseManifestFileStatus = orsRepository.findById(ARM_RESPONSE_MANIFEST_FAILED.getId()).orElseThrow();
        armIngestionStatus = orsRepository.findById(ARM_INGESTION.getId()).orElseThrow();
        armDropZoneStatus = orsRepository.findById(ARM_DROP_ZONE.getId()).orElseThrow();
        armResponseProcessingFailedStatus = orsRepository.findById(ARM_RESPONSE_PROCESSING_FAILED.getId()).orElseThrow();
        armResponseManifestFailedStatus = orsRepository.findById(ARM_RESPONSE_MANIFEST_FAILED.getId()).orElseThrow();
        armResponseChecksumVerificationFailedStatus = orsRepository.findById(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId()).orElseThrow();
        awaitingVerificationStatus = orsRepository.findById(AWAITING_VERIFICATION.getId()).orElseThrow();
        armRpoPendingStatus = orsRepository.findById(ARM_RPO_PENDING.getId()).orElseThrow();

        failedArmStatuses = List.of(failedArmRawDataStatus, failedArmManifestFileStatus, failedArmResponseManifestFileStatus);

    }

    public static boolean isEqual(ObjectRecordStatusEntity ors1, ObjectRecordStatusEntity ors2) {
        return ors1.getId().equals(ors2.getId());
    }

    public static boolean isEqual(ExternalLocationTypeEntity olt1, ExternalLocationTypeEntity olt2) {
        return olt1.getId().equals(olt2.getId());
    }

    public static boolean equalsAnyStatus(ObjectRecordStatusEntity ors, ObjectRecordStatusEntity... orsEntitiesToCompareTo) {
        return Arrays.stream(orsEntitiesToCompareTo).map(ObjectRecordStatusEntity::getId).anyMatch(orsToCompareTo -> orsToCompareTo.equals(ors.getId()));
    }

    @Transactional
    public void updateStatus(ObjectRecordStatusEntity newStatus, UserAccountEntity user, List<Integer> idsToBeUpdated, OffsetDateTime timestamp) {
        eodRepository.updateStatus(
            newStatus,
            user,
            idsToBeUpdated,
            timestamp
        );
    }
}