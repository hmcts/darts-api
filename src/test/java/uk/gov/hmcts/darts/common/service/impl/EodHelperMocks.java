package uk.gov.hmcts.darts.common.service.impl;

import lombok.SneakyThrows;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.util.EodHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Helper test class that mocks {@link EodHelper} entities and methods so that they can be used in unit tests.
 */
public class EodHelperMocks {

    @Mock private ExternalLocationTypeEntity armLocation;
    @Mock private ExternalLocationTypeEntity unstructuredLocation;
    @Mock private ExternalLocationTypeEntity inboundLocation;
    @Mock private ExternalLocationTypeEntity detsLocation;

    @Mock private ObjectRecordStatusEntity storedStatus;
    @Mock private ObjectRecordStatusEntity failureStatus;
    @Mock private ObjectRecordStatusEntity markForDeletionStatus;
    @Mock private ObjectRecordStatusEntity armIngestionStatus;
    @Mock private ObjectRecordStatusEntity armProcessingResponseFilesStatus;
    @Mock private ObjectRecordStatusEntity armDropZoneStatus;
    @Mock private ObjectRecordStatusEntity failedArmRawDataStatus;
    @Mock private ObjectRecordStatusEntity failedArmManifestFileStatus;
    @Mock private ObjectRecordStatusEntity awaitingVerificationStatus;

    private MockedStatic<EodHelper> mockedEodHelper;
    private AutoCloseable closeable;

    public EodHelperMocks() {
        mockEodHelper();
    }

    /**
     * Please note.
     * When mocking an EOD to return a certain location or status entity use the form
     * <pre>
     * doReturn(armLocation()).when(eod).getExternalLocationType();
     * </pre>
     * rather than the classic form
     * <pre>
     * when(eod.getExternalLocationType()).thenReturn(armLocation())
     * </pre>
     */
    public final void mockEodHelper() {
        closeable = MockitoAnnotations.openMocks(this);

        mockedEodHelper = Mockito.mockStatic(EodHelper.class);

        mockedEodHelper.when(EodHelper::armLocation).thenReturn(inboundLocation);
        lenient().when(inboundLocation.getId()).thenReturn(ExternalLocationTypeEnum.INBOUND.getId());
        lenient().when(inboundLocation.getDescription()).thenReturn("inbound");
        mockedEodHelper.when(EodHelper::armLocation).thenReturn(armLocation);
        lenient().when(armLocation.getId()).thenReturn(ExternalLocationTypeEnum.ARM.getId());
        lenient().when(armLocation.getDescription()).thenReturn("arm");
        mockedEodHelper.when(EodHelper::unstructuredLocation).thenReturn(unstructuredLocation);
        lenient().when(unstructuredLocation.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        lenient().when(unstructuredLocation.getDescription()).thenReturn("unstructured");
        mockedEodHelper.when(EodHelper::detsLocation).thenReturn(detsLocation);
        lenient().when(detsLocation.getId()).thenReturn(ExternalLocationTypeEnum.DETS.getId());
        lenient().when(detsLocation.getDescription()).thenReturn("dets");

        mockedEodHelper.when(EodHelper::storedStatus).thenReturn(storedStatus);
        lenient().when(storedStatus.getId()).thenReturn(ObjectRecordStatusEnum.STORED.getId());
        lenient().when(storedStatus.getDescription()).thenReturn("Stored");
        mockedEodHelper.when(EodHelper::failureStatus).thenReturn(failureStatus);
        lenient().when(failureStatus.getId()).thenReturn(ObjectRecordStatusEnum.FAILURE.getId());
        lenient().when(failureStatus.getDescription()).thenReturn("Failure");
        mockedEodHelper.when(EodHelper::markForDeletionStatus).thenReturn(markForDeletionStatus);
        lenient().when(markForDeletionStatus.getId()).thenReturn(ObjectRecordStatusEnum.MARKED_FOR_DELETION.getId());
        lenient().when(markForDeletionStatus.getDescription()).thenReturn("marked for Deletion");
        mockedEodHelper.when(EodHelper::armProcessingResponseFilesStatus).thenReturn(armProcessingResponseFilesStatus);
        lenient().when(armProcessingResponseFilesStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES.getId());
        lenient().when(armProcessingResponseFilesStatus.getDescription()).thenReturn("Arm Processing Response Files");
        mockedEodHelper.when(EodHelper::armIngestionStatus).thenReturn(armIngestionStatus);
        lenient().when(armIngestionStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_INGESTION.getId());
        lenient().when(armIngestionStatus.getDescription()).thenReturn("Arm Ingestion");
        mockedEodHelper.when(EodHelper::armDropZoneStatus).thenReturn(armDropZoneStatus);
        lenient().when(armDropZoneStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_DROP_ZONE.getId());
        lenient().when(armDropZoneStatus.getDescription()).thenReturn("Arm Drop Zone");
        mockedEodHelper.when(EodHelper::failedArmRawDataStatus).thenReturn(failedArmRawDataStatus);
        lenient().when(failedArmRawDataStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED.getId());
        lenient().when(failedArmRawDataStatus.getDescription()).thenReturn("Arm Raw Data Failed");
        mockedEodHelper.when(EodHelper::failedArmManifestFileStatus).thenReturn(failedArmManifestFileStatus);
        lenient().when(failedArmManifestFileStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_MANIFEST_FAILED.getId());
        lenient().when(failedArmManifestFileStatus.getDescription()).thenReturn("Arm Manifest Failed");
        mockedEodHelper.when(EodHelper::awaitingVerificationStatus).thenReturn(awaitingVerificationStatus);
        lenient().when(awaitingVerificationStatus.getId()).thenReturn(ObjectRecordStatusEnum.AWAITING_VERIFICATION.getId());
        lenient().when(awaitingVerificationStatus.getDescription()).thenReturn("Awaiting Verification");
    }

    public void givenIsEqualLocationReturns(boolean result) {
        mockedEodHelper.when(() -> EodHelper.isEqual(any(ExternalLocationTypeEntity.class), any(ExternalLocationTypeEntity.class)))
            .thenReturn(result);
    }

    public void givenIsEqualStatusReturns(boolean result) {
        mockedEodHelper.when(() -> EodHelper.isEqual(any(ObjectRecordStatusEntity.class), any(ObjectRecordStatusEntity.class)))
            .thenReturn(result);
    }

    @SneakyThrows
    public void close() {
        mockedEodHelper.close();
        closeable.close();
    }
}
