package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.impl.ArmResponseFilesProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.TestUtils;
import uk.gov.hmcts.darts.testutils.data.MediaTestData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@Transactional
class ArmResponseFilesProcessorIntTest extends IntegrationBase {

    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 6, 10);

    private ArmResponseFilesProcessor armResponseFilesProcessor;

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Autowired
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Autowired
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @MockBean
    private ArmDataManagementApi armDataManagementApi;
    @MockBean
    private FileOperationService fileOperationService;
    @MockBean
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @MockBean
    private UserIdentity userIdentity;

    @TempDir
    private File tempDirectory;

    @BeforeEach
    void setupData() {

        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();

        armResponseFilesProcessor = new ArmResponseFilesProcessorImpl(externalObjectDirectoryRepository,
                                                                      objectRecordStatusRepository,
                                                                      externalLocationTypeRepository,
                                                                      armDataManagementApi,
                                                                      fileOperationService,
                                                                      armDataManagementConfiguration,
                                                                      objectMapper,
                                                                      userIdentity);
    }

    @Test
    void givenProcessResponseFilesUnableToFindInputUploadFile() {
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectDirectoryStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        Map<String, BlobItem> responseBlobs = new HashMap<>();
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
    }

    @Test
    void givenProcessResponseFilesFoundInputUploadFilenameUnableToParseFilename() {
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectDirectoryStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String collectedBlobFilename = prefix + "_1_iu.rsp";
        Map<String, BlobItem> responseBlobs = new HashMap<>();
        responseBlobs.put(collectedBlobFilename, new BlobItem());

        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_RESPONSE_PROCESSING_FAILED.getId(), foundMedia.getStatus().getId());
    }

    @Test
    void givenProcessResponseFilesFoundInputUploadFileUnableToListBlobsForHashcode() {
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectDirectoryStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String collectedBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        Map<String, BlobItem> responseBlobs = new HashMap<>();
        responseBlobs.put(collectedBlobFilename, new BlobItem());

        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
    }

    @Test
    void givenProcessResponseFilesListsBlobsForHashcodeReturnsNoData() {
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectDirectoryStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        Map<String, BlobItem> inputUploadFilenameResponseBlobs = new HashMap<>();
        inputUploadFilenameResponseBlobs.put(inputUploadBlobFilename, new BlobItem());
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        Map<String, BlobItem> hashcodeResponseBlobs = new HashMap<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
    }

    @Test
    void givenProcessResponseFilesListsBlobsForHashcode() throws IOException {
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectDirectoryStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        Map<String, BlobItem> inputUploadFilenameResponseBlobs = new HashMap<>();
        inputUploadFilenameResponseBlobs.put(inputUploadBlobFilename, new BlobItem());
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        Map<String, BlobItem> hashcodeResponseBlobs = new HashMap<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        hashcodeResponseBlobs.put(createRecordFilename, new BlobItem());
        hashcodeResponseBlobs.put(uploadFileFilename, new BlobItem());
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/uploadFile/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getResponseBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryData);

        File uploadFileTestFile = new File(uploadFileTestFilename);
        Path uploadFilePath = Path.of(uploadFileTestFile.getAbsolutePath());
        when(fileOperationService.saveBinaryDataToSpecifiedWorkspace(
            uploadFileBinaryData,
            uploadFileFilename,
            armDataManagementConfiguration.getTempBlobWorkspace(),
            true
        )).thenReturn(uploadFilePath);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM));

        assertEquals(1, foundMediaList.size());
        //ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        //assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
    }


}
