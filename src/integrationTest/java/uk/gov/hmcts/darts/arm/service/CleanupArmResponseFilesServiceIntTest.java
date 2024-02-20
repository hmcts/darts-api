package uk.gov.hmcts.darts.arm.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.MediaTestData;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

class CleanupArmResponseFilesServiceIntTest extends IntegrationBase {

    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 9, 26);

    @MockBean
    private ArmDataManagementApi armDataManagementApi;
    @MockBean
    private UserIdentity userIdentity;
    @MockBean
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @MockBean
    private CurrentTimeHelper currentTimeHelper;

    @Autowired
    private CleanupArmResponseFilesService cleanupArmResponseFilesService;

    private MediaEntity savedMedia;
    private ExternalObjectDirectoryEntity armEod;

    @BeforeEach
    void setupData() {
        HearingEntity hearing = dartsDatabase.createHearing(
            "Bristol",
            "Int Test Courtroom 1",
            "1",
            HEARING_DATE
        );

        savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));
        dartsDatabase.save(savedMedia);

    }

    @Test
    void cleanupResponseFilesSuccessWithFiles_InputUpload_CreateRecord_UploadFile_AndStateStored() {
        armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setResponseCleaned(false);
        armEod = dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        String prefix = String.format("%d_%d_", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename = prefix + "1_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(List.of(inputUploadBlobFilename));

        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(List.of(createRecordFilename, uploadFileFilename));

        when(armDataManagementApi.deleteBlobData(any())).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        cleanupArmResponseFilesService.cleanupResponseFiles();

        Optional<ExternalObjectDirectoryEntity> foundMediaEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId());
        assertTrue(foundMediaEod.isPresent());
        ExternalObjectDirectoryEntity foundMedia = foundMediaEod.get();
        assertTrue(foundMedia.isResponseCleaned());
    }

    @Test
    void cleanupResponseFilesSuccessWithFiles_InputUpload_CreateRecord_UploadFileAndStateManifestFailed() {

        armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_RESPONSE_MANIFEST_FAILED,
            ARM,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setResponseCleaned(false);
        armEod = dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        String prefix = String.format("%d_%d_", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename = prefix + "1_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(List.of(inputUploadBlobFilename));

        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(List.of(createRecordFilename, uploadFileFilename));

        when(armDataManagementApi.deleteBlobData(inputUploadBlobFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);


        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        cleanupArmResponseFilesService.cleanupResponseFiles();

        Optional<ExternalObjectDirectoryEntity> foundMediaEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId());
        assertTrue(foundMediaEod.isPresent());
        ExternalObjectDirectoryEntity foundMedia = foundMediaEod.get();
        assertTrue(foundMedia.isResponseCleaned());
    }

    @Test
    void cleanupResponseFilesSuccessWithFiles_InputUpload_CreateRecord_UploadFile_AndStateArmResponseProcessingFailed() {

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_RESPONSE_PROCESSING_FAILED,
            ARM,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setResponseCleaned(false);
        armEod = dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        String prefix = String.format("%d_%d_", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename = prefix + "1_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(List.of(inputUploadBlobFilename));

        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(List.of(createRecordFilename, uploadFileFilename));

        when(armDataManagementApi.deleteBlobData(any())).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        cleanupArmResponseFilesService.cleanupResponseFiles();

        Optional<ExternalObjectDirectoryEntity> foundMediaEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId());
        assertTrue(foundMediaEod.isPresent());
        ExternalObjectDirectoryEntity foundMedia = foundMediaEod.get();
        assertTrue(foundMedia.isResponseCleaned());
    }

    @Test
    void cleanupResponseFilesSuccessWithFiles_InputUpload_CreateRecord_UploadFile_AndStateArmResponseChecksumFailed() {

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED,
            ARM,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setResponseCleaned(false);
        armEod = dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        String prefix = String.format("%d_%d_", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename = prefix + "1_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(List.of(inputUploadBlobFilename));

        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(List.of(createRecordFilename, uploadFileFilename));

        when(armDataManagementApi.deleteBlobData(any())).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        cleanupArmResponseFilesService.cleanupResponseFiles();

        Optional<ExternalObjectDirectoryEntity> foundMediaEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId());
        assertTrue(foundMediaEod.isPresent());
        ExternalObjectDirectoryEntity foundMedia = foundMediaEod.get();
        assertTrue(foundMedia.isResponseCleaned());
    }

    @Test
    void cleanupResponseFilesSuccessWithFiles_InputUpload_CreateRecord_UploadFile_ForMultipleAttempts() {

        armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setResponseCleaned(false);
        armEod = dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        String prefix = String.format("%d_%d_", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename1 = prefix + "1_6a374f19a9ce7dc9cc480ea8d4eca0fb_0_iu.rsp";
        String inputUploadBlobFilename2 = prefix + "2_7a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(List.of(inputUploadBlobFilename1, inputUploadBlobFilename2));

        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_0_cr.rsp";
        String uploadFileFilename1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_uf.rsp";
        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(List.of(createRecordFilename1, uploadFileFilename1));

        String hashcode2 = "7a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename2 = "7a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_0_cr.rsp";
        String uploadFileFilename2 = "7a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_uf.rsp";
        when(armDataManagementApi.listResponseBlobs(hashcode2)).thenReturn(List.of(createRecordFilename2, uploadFileFilename2));

        when(armDataManagementApi.deleteBlobData(any())).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        cleanupArmResponseFilesService.cleanupResponseFiles();

        Optional<ExternalObjectDirectoryEntity> foundMediaEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId());
        assertTrue(foundMediaEod.isPresent());
        ExternalObjectDirectoryEntity foundMedia = foundMediaEod.get();
        assertTrue(foundMedia.isResponseCleaned());
    }

    @Test
    void cleanupResponseFilesSuccessWithFiles_InputUpload_CreateRecord_InvalidLine() {
        armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setResponseCleaned(false);
        armEod = dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        String prefix = String.format("%d_%d_", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename = prefix + "1_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(List.of(inputUploadBlobFilename));

        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String invalidLineFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_il.rsp";
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(List.of(createRecordFilename, invalidLineFilename));

        when(armDataManagementApi.deleteBlobData(any())).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        cleanupArmResponseFilesService.cleanupResponseFiles();

        Optional<ExternalObjectDirectoryEntity> foundMediaEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId());
        assertTrue(foundMediaEod.isPresent());
        ExternalObjectDirectoryEntity foundMedia = foundMediaEod.get();
        assertTrue(foundMedia.isResponseCleaned());
    }

    @Test
    void cleanupResponseFilesFailsWithOnlyResponseFileInputUpload() {

        armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setResponseCleaned(false);
        armEod = dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        String prefix = String.format("%d_%d_", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename = prefix + "1_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(List.of(inputUploadBlobFilename));

        when(armDataManagementApi.deleteBlobData(any())).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        cleanupArmResponseFilesService.cleanupResponseFiles();

        Optional<ExternalObjectDirectoryEntity> foundMediaEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId());
        assertTrue(foundMediaEod.isPresent());
        ExternalObjectDirectoryEntity foundMedia = foundMediaEod.get();
        assertFalse(foundMedia.isResponseCleaned());
    }

    @Test
    void cleanupResponseFilesWith2InputUploadResponseFilesWithDifferentTransferAttempts() {

        armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(2);
        armEod.setResponseCleaned(false);
        armEod = dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        String prefix = String.format("%d_%d_", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilenameTransferAttempt1 = prefix + "1_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        String inputUploadBlobFilenameTransferAttempt2 = prefix + "2_7a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(List.of(inputUploadBlobFilenameTransferAttempt1,
                                                                                inputUploadBlobFilenameTransferAttempt2));

        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String invalidLineFilename1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_il.rsp";
        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(List.of(createRecordFilename1, invalidLineFilename1));

        String hashcode2 = "7a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename2 = "7a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String invalidLineFilename2 = "7a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_il.rsp";
        when(armDataManagementApi.listResponseBlobs(hashcode2)).thenReturn(List.of(createRecordFilename2, invalidLineFilename2));

        when(armDataManagementApi.deleteBlobData(any())).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        cleanupArmResponseFilesService.cleanupResponseFiles();

        Optional<ExternalObjectDirectoryEntity> foundMediaEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId());
        assertTrue(foundMediaEod.isPresent());
        ExternalObjectDirectoryEntity foundMedia = foundMediaEod.get();
        assertTrue(foundMedia.isResponseCleaned());
    }

    @Test
    void cleanupResponseFilesFailsToDeleteResponseFiles() {

        armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setResponseCleaned(false);
        armEod = dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        String prefix = String.format("%d_%d_", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename = prefix + "1_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(List.of(inputUploadBlobFilename));

        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(List.of(createRecordFilename, uploadFileFilename));

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(10);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(armDataManagementApi.deleteBlobData(any())).thenReturn(false);

        cleanupArmResponseFilesService.cleanupResponseFiles();

        Optional<ExternalObjectDirectoryEntity> foundMediaEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId());
        assertTrue(foundMediaEod.isPresent());
        ExternalObjectDirectoryEntity foundMedia = foundMediaEod.get();
        assertFalse(foundMedia.isResponseCleaned());
    }
}
