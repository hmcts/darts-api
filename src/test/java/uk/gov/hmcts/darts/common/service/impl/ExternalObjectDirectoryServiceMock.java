package uk.gov.hmcts.darts.common.service.impl;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.mockito.Mock;
import uk.gov.hmcts.darts.arm.service.impl.EodEntities;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;

import static org.mockito.Mockito.when;

@Getter
@Accessors(fluent = true)
public class ExternalObjectDirectoryServiceMock {

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

    public void givenServiceIsMocked(EodEntities mockEodService) {
//        MockitoAnnotations.initMocks(this);
//
//        when(mockEodService.armLocation()).thenReturn(armLocation);
//
//        when(mockEodService.unstructuredLocation()).thenReturn(unstructuredLocation);
//        when(mockEodService.armLocation()).thenReturn(armLocation);
//
//        when(mockEodService.storedStatus()).thenReturn(storedStatus);
//        when(mockEodService.armIngestionStatus()).thenReturn(armIngestionStatus);
//        when(mockEodService.failedArmRawDataStatus()).thenReturn(failedArmRawDataStatus);
//        when(mockEodService.failedArmManifestFileStatus()).thenReturn(failedArmManifestFileStatus);
//        when(mockEodService.armDropZoneStatus()).thenReturn(armDropZoneStatus);

        when(unstructuredLocation.getId()).thenReturn(ExternalLocationTypeEnum.UNSTRUCTURED.getId());

        when(eodUnstructured.getExternalLocationType()).thenReturn(unstructuredLocation);
        when(eodArm.getExternalLocationType()).thenReturn(armLocation);
    }
}
