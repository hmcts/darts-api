package uk.gov.hmcts.darts.arm.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.impl.CleanupArmResponseFilesServiceImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.MediaTestData;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
class CleanupArmResponseFilesServiceIntTest extends IntegrationBase {

    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 9, 26);

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Autowired
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Autowired
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @MockBean
    private ArmDataManagementApi armDataManagementApi;
    @MockBean
    private UserIdentity userIdentity;
    @MockBean
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @MockBean
    private CurrentTimeHelper currentTimeHelper;

    private CleanupArmResponseFilesService cleanupArmResponseFilesService;

    @BeforeEach
    void setupData() {

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
    void cleanupResponseFilesSuccessWithFiles_IU_CR_UF() {

        HearingEntity hearing = dartsDatabase.createHearing(
            "Bristol",
            "Int Test Courtroom 1",
            "1",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));
        savedMedia.setChecksum("C3CCA7021CF79B42F245AF350601C284");
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setResponseCleaned(false);
        armEod = dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

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
    void cleanupResponseFilesSuccessWithFiles_IU_CR_IL() {

        HearingEntity hearing = dartsDatabase.createHearing(
            "Bristol",
            "Int Test Courtroom 1",
            "1",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));
        savedMedia.setChecksum("C3CCA7021CF79B42F245AF350601C284");
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setResponseCleaned(false);
        armEod = dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_il.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

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
    void cleanupResponseFilesFailsToDeleteWithOnlyResponseFileIU() {

        HearingEntity hearing = dartsDatabase.createHearing(
            "Bristol",
            "Int Test Courtroom 1",
            "1",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));
        savedMedia.setChecksum("C3CCA7021CF79B42F245AF350601C284");
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setResponseCleaned(false);
        armEod = dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

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
    void cleanupResponseFilesFailesToDeleteResponseFiles() {

        HearingEntity hearing = dartsDatabase.createHearing(
            "Bristol",
            "Int Test Courtroom 1",
            "1",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));
        savedMedia.setChecksum("C3CCA7021CF79B42F245AF350601C284");
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setResponseCleaned(false);
        armEod = dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getResponseCleanupBufferDays()).thenReturn(0);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

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
}
