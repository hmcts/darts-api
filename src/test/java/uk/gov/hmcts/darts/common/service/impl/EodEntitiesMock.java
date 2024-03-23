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
import uk.gov.hmcts.darts.common.util.EodEntities;

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

    public void givenEodEntitiesAreMocked() {
        MockitoAnnotations.initMocks(this);

        MockedStatic<EodEntities> utilities = Mockito.mockStatic(EodEntities.class);
        utilities.when(EodEntities::armLocation).thenReturn(armLocation);
        utilities.when(EodEntities::unstructuredLocation).thenReturn(unstructuredLocation);

        utilities.when(EodEntities::storedStatus).thenReturn(storedStatus);
        utilities.when(EodEntities::armIngestionStatus).thenReturn(armIngestionStatus);
        utilities.when(EodEntities::failedArmRawDataStatus).thenReturn(failedArmRawDataStatus);
        utilities.when(EodEntities::failedArmManifestFileStatus).thenReturn(failedArmManifestFileStatus);
        utilities.when(EodEntities::armDropZoneStatus).thenReturn(armDropZoneStatus);

//        when(EodEntities.armLocation()).thenReturn(armLocation);
//
//        when(EodEntities.unstructuredLocation()).thenReturn(unstructuredLocation);
//        when(EodEntities.armLocation()).thenReturn(armLocation);
//
//        when(EodEntities.storedStatus()).thenReturn(storedStatus);
//        when(EodEntities.armIngestionStatus()).thenReturn(armIngestionStatus);
//        when(EodEntities.failedArmRawDataStatus()).thenReturn(failedArmRawDataStatus);
//        when(EodEntities.failedArmManifestFileStatus()).thenReturn(failedArmManifestFileStatus);
//        when(EodEntities.armDropZoneStatus()).thenReturn(armDropZoneStatus);

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
}
