package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.impl.CleanupArmResponseFilesServiceImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanupArmResponseFilesServiceImplTest {

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @Mock
    private ExternalLocationTypeEntity externalLocationTypeArm;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusStored;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusArmResponseChecksumFailed;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusArmResponseProcessingFailed;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusArmResponseManifestFailed;
    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectory;


    private CleanupArmResponseFilesService cleanupArmResponseFilesService;

    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;


    @BeforeEach
    void setUp() {
        cleanupArmResponseFilesService = new CleanupArmResponseFilesServiceImpl(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            armDataManagementApi,
            userIdentity,
            armDataManagementConfiguration,
            currentTimeHelper
        );
    }

    @Test
    @Disabled
    void cleanupResponseFiles() {

        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        when(objectRecordStatusRepository.findById(2)).thenReturn(Optional.of(objectRecordStatusStored));
        when(objectRecordStatusRepository.findById(17)).thenReturn(Optional.of(objectRecordStatusArmResponseProcessingFailed));
        when(objectRecordStatusRepository.findById(18)).thenReturn(Optional.of(objectRecordStatusArmResponseChecksumFailed));
        when(objectRecordStatusRepository.findById(15)).thenReturn(Optional.of(objectRecordStatusArmResponseManifestFailed));

        when(externalObjectDirectory.getId()).thenReturn(1);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(externalObjectDirectoryRepository.findByStatusInAndStorageLocationAndResponseCleanedAndLastModifiedDateTimeBefore(
            any(),
            any(),
            anyBoolean(),
            any()
        )).thenReturn(List.of(externalObjectDirectory));

        cleanupArmResponseFilesService.cleanupResponseFiles();

        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

    }
}