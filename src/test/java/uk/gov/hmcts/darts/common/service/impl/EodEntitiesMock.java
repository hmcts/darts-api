package uk.gov.hmcts.darts.common.service.impl;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.util.EodHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class EodEntitiesMock {

    @Mock(lenient = true)
    private ExternalLocationTypeEntity armLocation;
    @Mock(lenient = true)
    private ExternalLocationTypeEntity unstructuredLocation;
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
    @Mock(lenient = true)
    private ExternalObjectDirectoryEntity eodUnstructured;
    @Mock(lenient = true)
    private ExternalObjectDirectoryEntity eodArm;

    private MockedStatic<EodHelper> mockedEodEntities;

    public void givenEodEntitiesAreMocked() {
        MockitoAnnotations.initMocks(this);

        mockedEodEntities = Mockito.mockStatic(EodHelper.class);
        mockedEodEntities.when(EodHelper::armLocation).thenReturn(armLocation);
        mockedEodEntities.when(EodHelper::unstructuredLocation).thenReturn(unstructuredLocation);

        mockedEodEntities.when(EodHelper::storedStatus).thenReturn(storedStatus);
        mockedEodEntities.when(EodHelper::armIngestionStatus).thenReturn(armIngestionStatus);
        mockedEodEntities.when(EodHelper::failedArmRawDataStatus).thenReturn(failedArmRawDataStatus);
        mockedEodEntities.when(EodHelper::failedArmManifestFileStatus).thenReturn(failedArmManifestFileStatus);
        mockedEodEntities.when(EodHelper::armDropZoneStatus).thenReturn(armDropZoneStatus);

        when(armLocation.getId()).thenReturn(ExternalLocationTypeEnum.ARM.getId());
        when(unstructuredLocation.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());

        when(storedStatus.getId()).thenReturn(ObjectRecordStatusEnum.STORED.getId());
        when(armIngestionStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_INGESTION.getId());
        when(armDropZoneStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_DROP_ZONE.getId());
        when(failedArmRawDataStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED.getId());
        when(failedArmManifestFileStatus.getId()).thenReturn(ObjectRecordStatusEnum.ARM_MANIFEST_FAILED.getId());

        when(eodUnstructured.getExternalLocationType()).thenReturn(unstructuredLocation);
        when(eodArm.getExternalLocationType()).thenReturn(armLocation);
    }

    public void givenIsEqualLocationReturns(boolean result) {
        mockedEodEntities.when(() -> EodHelper.isEqual(any(ExternalLocationTypeEntity.class), any(ExternalLocationTypeEntity.class)))
            .thenReturn(result);
    }

    public void givenIsEqualStatusReturns(boolean result) {
        mockedEodEntities.when(() -> EodHelper.isEqual(any(ObjectRecordStatusEntity.class), any(ObjectRecordStatusEntity.class)))
            .thenReturn(result);
    }

    public void close() {
        mockedEodEntities.close();
    }
}
