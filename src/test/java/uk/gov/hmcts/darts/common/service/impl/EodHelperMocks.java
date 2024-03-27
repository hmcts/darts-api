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

    @Mock
    private ExternalLocationTypeEntity armLocation;
    @Mock
    private ExternalLocationTypeEntity unstructuredLocation;
    @Mock
    private ExternalLocationTypeEntity detsLocation;
    @Mock
    private ObjectRecordStatusEntity storedStatus;
    @Mock
    private ObjectRecordStatusEntity armIngestionStatus;
    @Mock
    private ObjectRecordStatusEntity armDropZoneStatus;
    @Mock
    private ObjectRecordStatusEntity failedArmRawDataStatus;
    @Mock
    private ObjectRecordStatusEntity failedArmManifestFileStatus;

    private MockedStatic<EodHelper> mockedEodHelper;
    private AutoCloseable closeable;

    public EodHelperMocks() {
        mockEodHelper();
    }

    public final void mockEodHelper() {
        closeable = MockitoAnnotations.openMocks(this);

        mockedEodHelper = Mockito.mockStatic(EodHelper.class);

        mockedEodHelper.when(EodHelper::armLocation).thenReturn(armLocation);
        mockedEodHelper.when(EodHelper::unstructuredLocation).thenReturn(unstructuredLocation);
        mockedEodHelper.when(EodHelper::detsLocation).thenReturn(detsLocation);

        lenient().when(armLocation.getId()).thenReturn(ExternalLocationTypeEnum.ARM.getId());
        lenient().when(armLocation.getDescription()).thenReturn("arm");
        lenient().when(unstructuredLocation.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        lenient().when(unstructuredLocation.getDescription()).thenReturn("unstructured");
        lenient().when(unstructuredLocation.getId()).thenReturn(ExternalLocationTypeEnum.DETS.getId());
        lenient().when(unstructuredLocation.getDescription()).thenReturn("dets");

        mockedEodHelper.when(EodHelper::storedStatus).thenReturn(storedStatus);
        mockedEodHelper.when(EodHelper::armIngestionStatus).thenReturn(armIngestionStatus);
        mockedEodHelper.when(EodHelper::failedArmRawDataStatus).thenReturn(failedArmRawDataStatus);
        mockedEodHelper.when(EodHelper::failedArmManifestFileStatus).thenReturn(failedArmManifestFileStatus);
        mockedEodHelper.when(EodHelper::armDropZoneStatus).thenReturn(armDropZoneStatus);

        lenient().when(storedStatus.getId()).thenReturn(ObjectRecordStatusEnum.STORED.getId());
        lenient().when(storedStatus.getDescription()).thenReturn("Stored");
        lenient().when(armIngestionStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_INGESTION.getId());
        lenient().when(armIngestionStatus.getDescription()).thenReturn("Arm Ingestion");
        lenient().when(armDropZoneStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_DROP_ZONE.getId());
        lenient().when(armDropZoneStatus.getDescription()).thenReturn("Arm Drop Zone");
        lenient().when(failedArmRawDataStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED.getId());
        lenient().when(failedArmRawDataStatus.getDescription()).thenReturn("Arm Raw Data Failed");
        lenient().when(failedArmManifestFileStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_MANIFEST_FAILED.getId());
        lenient().when(failedArmManifestFileStatus.getDescription()).thenReturn("Arm Manifest Failed");
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
