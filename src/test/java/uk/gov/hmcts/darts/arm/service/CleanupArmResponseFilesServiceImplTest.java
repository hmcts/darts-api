package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.testutils.ExternalObjectDirectoryTestData;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    private ObjectRecordStatusEntity objectRecordStatusArmRpoPending;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusArmResponseChecksumFailed;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusArmResponseProcessingFailed;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusArmResponseManifestFailed;

    private ExternalObjectDirectoryEntity externalObjectDirectoryEntity;

    @Mock
    private MediaEntity media;
    @Mock
    private UserAccountEntity testUser;

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

        externalObjectDirectoryEntity = new ExternalObjectDirectoryTestData().createExternalObjectDirectory(
            media,
            ExternalLocationTypeEnum.ARM,
            ObjectRecordStatusEnum.STORED,
            UUID.randomUUID());
    }

    @Test
    void cleanupResponseFilesSuccess() {
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusStored);
        when(objectRecordStatusRepository.getReferenceById(17)).thenReturn(objectRecordStatusArmResponseProcessingFailed);
        when(objectRecordStatusRepository.getReferenceById(18)).thenReturn(objectRecordStatusArmResponseChecksumFailed);
        when(objectRecordStatusRepository.getReferenceById(19)).thenReturn(objectRecordStatusArmResponseManifestFailed);
        when(objectRecordStatusRepository.getReferenceById(21)).thenReturn(objectRecordStatusArmRpoPending);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        when(media.getId()).thenReturn(456);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(externalObjectDirectoryRepository.findSingleArmResponseFiles(
            List.of(objectRecordStatusStored,
                    objectRecordStatusArmRpoPending,
                    objectRecordStatusArmResponseManifestFailed,
                    objectRecordStatusArmResponseProcessingFailed,
                    objectRecordStatusArmResponseChecksumFailed),
            externalLocationTypeArm,
            false,
            testTime,
            armDataManagementConfiguration.getManifestFilePrefix()
        )).thenReturn(List.of(externalObjectDirectoryEntity));

        String inputUploadBlobFilename = "123_456_1_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadBlobFilename);
        when(armDataManagementApi.listResponseBlobs("123_456_")).thenReturn(inputUploadFilenameResponseBlobs);

        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(List.of(createRecordFilename, uploadFileFilename));

        when(armDataManagementApi.deleteBlobData(any())).thenReturn(true);

        when(userIdentity.getUserAccount()).thenReturn(testUser);

        cleanupArmResponseFilesService.cleanupResponseFiles();

        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        assertTrue(externalObjectDirectoryEntity.isResponseCleaned());
    }

    @Test
    void cleanupResponseFiles_WithInputUploadFileNoOtherResponseFiles() {
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusStored);
        when(objectRecordStatusRepository.getReferenceById(17)).thenReturn(objectRecordStatusArmResponseProcessingFailed);
        when(objectRecordStatusRepository.getReferenceById(18)).thenReturn(objectRecordStatusArmResponseChecksumFailed);
        when(objectRecordStatusRepository.getReferenceById(19)).thenReturn(objectRecordStatusArmResponseManifestFailed);
        when(objectRecordStatusRepository.getReferenceById(21)).thenReturn(objectRecordStatusArmRpoPending);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        when(media.getId()).thenReturn(456);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(externalObjectDirectoryRepository.findSingleArmResponseFiles(
            List.of(objectRecordStatusStored,
                    objectRecordStatusArmRpoPending,
                    objectRecordStatusArmResponseManifestFailed,
                    objectRecordStatusArmResponseProcessingFailed,
                    objectRecordStatusArmResponseChecksumFailed),
            externalLocationTypeArm,
            false,
            testTime,
            armDataManagementConfiguration.getManifestFilePrefix()
        )).thenReturn(List.of(externalObjectDirectoryEntity));

        String inputUploadBlobFilename = "123_456_1_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadBlobFilename);
        when(armDataManagementApi.listResponseBlobs("123_456_")).thenReturn(inputUploadFilenameResponseBlobs);

        when(userIdentity.getUserAccount()).thenReturn(testUser);

        cleanupArmResponseFilesService.cleanupResponseFiles();

        assertFalse(externalObjectDirectoryEntity.isResponseCleaned());
    }
}