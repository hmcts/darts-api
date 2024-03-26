package uk.gov.hmcts.darts.common.service.impl;

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
import static org.mockito.Mockito.when;

public class EodHelperMocks {

    @Mock(lenient = true)
    private ExternalLocationTypeEntity armLocation;
    @Mock(lenient = true)
    private ExternalLocationTypeEntity unstructuredLocation;
    @Mock(lenient = true)
    private ExternalLocationTypeEntity detsLocation;
    @Mock(lenient = true)
    private ObjectRecordStatusEntity storedStatus;
    @Mock(lenient = true)
    private ObjectRecordStatusEntity armIngestionStatus;
    @Mock(lenient = true)
    private ObjectRecordStatusEntity armDropZoneStatus;
    @Mock(lenient = true)
    private ObjectRecordStatusEntity failedArmRawDataStatus;
    @Mock(lenient = true)
    private ObjectRecordStatusEntity failedArmManifestFileStatus;

    private MockedStatic<EodHelper> mockedEodHelper;

    public EodHelperMocks() {
        mockEodHelper();
    }

    public void mockEodHelper() {
        MockitoAnnotations.initMocks(this);

        mockedEodHelper = Mockito.mockStatic(EodHelper.class);

        mockedEodHelper.when(EodHelper::armLocation).thenReturn(armLocation);
        mockedEodHelper.when(EodHelper::unstructuredLocation).thenReturn(unstructuredLocation);
        mockedEodHelper.when(EodHelper::detsLocation).thenReturn(detsLocation);

        when(armLocation.getId()).thenReturn(ExternalLocationTypeEnum.ARM.getId());
        when(armLocation.getDescription()).thenReturn("arm");
        when(unstructuredLocation.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(unstructuredLocation.getDescription()).thenReturn("unstructured");
        when(unstructuredLocation.getId()).thenReturn(ExternalLocationTypeEnum.DETS.getId());
        when(unstructuredLocation.getDescription()).thenReturn("dets");

        mockedEodHelper.when(EodHelper::storedStatus).thenReturn(storedStatus);
        mockedEodHelper.when(EodHelper::armIngestionStatus).thenReturn(armIngestionStatus);
        mockedEodHelper.when(EodHelper::failedArmRawDataStatus).thenReturn(failedArmRawDataStatus);
        mockedEodHelper.when(EodHelper::failedArmManifestFileStatus).thenReturn(failedArmManifestFileStatus);
        mockedEodHelper.when(EodHelper::armDropZoneStatus).thenReturn(armDropZoneStatus);

        when(storedStatus.getId()).thenReturn(ObjectRecordStatusEnum.STORED.getId());
        when(storedStatus.getDescription()).thenReturn("Stored");
        when(armIngestionStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_INGESTION.getId());
        when(armIngestionStatus.getDescription()).thenReturn("Arm Ingestion");
        when(armDropZoneStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_DROP_ZONE.getId());
        when(armDropZoneStatus.getDescription()).thenReturn("Arm Drop Zone");
        when(failedArmRawDataStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED.getId());
        when(failedArmRawDataStatus.getDescription()).thenReturn("Arm Raw Data Failed");
        when(failedArmManifestFileStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_MANIFEST_FAILED.getId());
        when(failedArmManifestFileStatus.getDescription()).thenReturn("Arm Manifest Failed");
    }

    public void givenIsEqualLocationReturns(boolean result) {
        mockedEodHelper.when(() -> EodHelper.isEqual(any(ExternalLocationTypeEntity.class), any(ExternalLocationTypeEntity.class)))
            .thenReturn(result);
    }

    public void givenIsEqualStatusReturns(boolean result) {
        mockedEodHelper.when(() -> EodHelper.isEqual(any(ObjectRecordStatusEntity.class), any(ObjectRecordStatusEntity.class)))
            .thenReturn(result);
    }

    public void close() {
        mockedEodHelper.close();
    }
}
