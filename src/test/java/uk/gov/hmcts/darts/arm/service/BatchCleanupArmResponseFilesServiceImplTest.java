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
import uk.gov.hmcts.darts.arm.exception.UnableToReadArmFileException;
import uk.gov.hmcts.darts.arm.helper.ArmResponseFileHelper;
import uk.gov.hmcts.darts.arm.model.InputUploadAndAssociatedFilenames;
import uk.gov.hmcts.darts.arm.service.impl.BatchCleanupArmResponseFilesServiceImpl;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchCleanupArmResponseFilesServiceImplTest {

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

    private ExternalObjectDirectoryEntity eodEntityForUiFile;

    @Mock
    private MediaEntity media;
    @Mock
    private UserAccountEntity testUser;
    @Mock
    private ArmResponseFileHelper armResponseFileHelper;

    private BatchCleanupArmResponseFilesService cleanupArmResponseFilesService;

    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;


    @BeforeEach
    void setUp() {
        cleanupArmResponseFilesService = new BatchCleanupArmResponseFilesServiceImpl(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            armDataManagementApi,
            userIdentity,
            armDataManagementConfiguration,
            currentTimeHelper,
            armResponseFileHelper
        );

        eodEntityForUiFile = ExternalObjectDirectoryTestData.createExternalObjectDirectory(
            media,
            ExternalLocationTypeEnum.ARM,
            ObjectRecordStatusEnum.STORED,
            UUID.randomUUID());

        eodEntityForUiFile.setManifestFile("DARTS_00000001.a360");

        when(objectRecordStatusRepository.getReferencesByStatus(anyList())).thenReturn(List.of(objectRecordStatusStored,
                                                                                               objectRecordStatusArmResponseManifestFailed,
                                                                                               objectRecordStatusArmResponseProcessingFailed,
                                                                                               objectRecordStatusArmResponseChecksumFailed
        ));
        when(armDataManagementConfiguration.getBatchResponseCleanupBufferMinutes()).thenReturn(15);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn("DARTS_");
    }

    @Test
    void cleanupResponseFilesSuccess() throws UnableToReadArmFileException {
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(externalObjectDirectoryRepository.findBatchArmResponseFiles(
            eq(List.of(objectRecordStatusStored,
                       objectRecordStatusArmResponseManifestFailed,
                       objectRecordStatusArmResponseProcessingFailed,
                       objectRecordStatusArmResponseChecksumFailed)),
            eq(externalLocationTypeArm),
            eq(false),
            any(OffsetDateTime.class),
            anyString(),
            eq(100)
        )).thenReturn(List.of(eodEntityForUiFile));

        String inputUploadBlobFilename = "123_456_1_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadBlobFilename);

        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";

        ExternalObjectDirectoryEntity eodEntityForAssociatedFiles1 = ExternalObjectDirectoryTestData.createExternalObjectDirectory(
            media,
            ExternalLocationTypeEnum.ARM,
            ObjectRecordStatusEnum.STORED,
            UUID.randomUUID());
        eodEntityForAssociatedFiles1.setId(1);

        InputUploadAndAssociatedFilenames inputUploadAndAssociatedFilenames = new InputUploadAndAssociatedFilenames();
        inputUploadAndAssociatedFilenames.setInputUploadFilename(inputUploadBlobFilename);
        inputUploadAndAssociatedFilenames.addAssociatedFile(eodEntityForAssociatedFiles1.getId(), createRecordFilename);
        inputUploadAndAssociatedFilenames.addAssociatedFile(eodEntityForAssociatedFiles1.getId(), uploadFileFilename);
        when(armResponseFileHelper.getCorrespondingArmFilesForManifestFilename(anyString())).thenReturn(List.of(inputUploadAndAssociatedFilenames));


        when(externalObjectDirectoryRepository.findById(eodEntityForAssociatedFiles1.getId())).thenReturn(Optional.of(eodEntityForAssociatedFiles1));

        when(armDataManagementApi.deleteBlobData(any())).thenReturn(true);

        when(userIdentity.getUserAccount()).thenReturn(testUser);

        cleanupArmResponseFilesService.cleanupResponseFiles(100);

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        assertTrue(eodEntityForUiFile.isResponseCleaned());
        assertTrue(eodEntityForAssociatedFiles1.isResponseCleaned());
    }

    @Test
    void cleanupResponseFilesSuccess_NoCorrespondingFiles() throws UnableToReadArmFileException {
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(externalObjectDirectoryRepository.findBatchArmResponseFiles(
            eq(List.of(objectRecordStatusStored,
                       objectRecordStatusArmResponseManifestFailed,
                       objectRecordStatusArmResponseProcessingFailed,
                       objectRecordStatusArmResponseChecksumFailed)),
            eq(externalLocationTypeArm),
            eq(false),
            any(OffsetDateTime.class),
            anyString(),
            eq(100)
        )).thenReturn(List.of(eodEntityForUiFile));

        String inputUploadBlobFilename = "123_456_1_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadBlobFilename);

        InputUploadAndAssociatedFilenames inputUploadAndAssociatedFilenames = new InputUploadAndAssociatedFilenames();
        inputUploadAndAssociatedFilenames.setInputUploadFilename(inputUploadBlobFilename);
        when(armResponseFileHelper.getCorrespondingArmFilesForManifestFilename(anyString())).thenReturn(List.of(inputUploadAndAssociatedFilenames));


        when(armDataManagementApi.deleteBlobData(any())).thenReturn(true);

        when(userIdentity.getUserAccount()).thenReturn(testUser);

        cleanupArmResponseFilesService.cleanupResponseFiles(100);

        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        assertTrue(eodEntityForUiFile.isResponseCleaned());
    }
}