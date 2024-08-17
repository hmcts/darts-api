package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArmResponseFilesProcessSingleElement;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.impl.ArmResponseFilesProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.MediaTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;


@Disabled("Impacted by V1_367__adding_not_null_constraints_part_4.sql")
class ArmResponseFilesProcessorIntTest extends IntegrationBase {

    private static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 6, 10, 10, 0, 0);

    private ArmResponseFilesProcessor armResponseFilesProcessor;

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @MockBean
    private ArmDataManagementApi armDataManagementApi;
    @MockBean
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @MockBean
    private UserIdentity userIdentity;

    @Autowired
    private ArmResponseFilesProcessSingleElement armResponseFilesProcessSingleElement;

    @Autowired
    private AuthorisationStub authorisationStub;

    @TempDir
    private File tempDirectory;

    @BeforeEach
    void setupData() {

        armResponseFilesProcessor = new ArmResponseFilesProcessorImpl(
            externalObjectDirectoryRepository,
            userIdentity,
            armResponseFilesProcessSingleElement
        );
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
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        List<String> responseBlobs = new ArrayList<>();
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());

    }

    @Test
    void givenProcessResponseFilesGetInputUploadFileThrowsException() {
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
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        when(armDataManagementApi.listResponseBlobs(prefix)).thenThrow(new BlobStorageException("Failed", null, null));

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(2, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());
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
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String responseFilename = prefix + "_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_RESPONSE_PROCESSING_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void givenProcessResponseFilesFailOnUnableToParseFilenameAndReachedMaxAttempts() {
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
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        armEod.setVerificationAttempts(3);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String responseFilename = prefix + "_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_RESPONSE_PROCESSING_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(3, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verifyNoMoreInteractions(armDataManagementApi);
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
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String responseFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());
    }

    @Test
    void givenProcessResponseFilesFoundInputUploadFileThrowsExceptionWhenListBlobsForHashcode() {
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
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        when(armDataManagementApi.listResponseBlobs(prefix)).thenThrow(new BlobStorageException("Failed", null, null));

        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(2, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());
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
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());
    }

    @Test
    void givenProcessResponseFilesInputOutputFileReportsFailed() throws IOException {
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
        savedMedia.setChecksum("C3CCA7021CF79B42F245AF350601C284");
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_0_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_0_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/uploadFile/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_uf.rsp";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryData);

        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_RESPONSE_PROCESSING_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertTrue(foundMedia.isResponseCleaned());
    }

    @Test
    void givenProcessResponseFilesFailsStatusCheck() throws IOException {
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
        savedMedia.setChecksum("123");
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_0_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_0_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/uploadFile/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryData);

        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_RESPONSE_PROCESSING_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertTrue(foundMedia.isResponseCleaned());
    }

    @Test
    void givenProcessResponseFilesFailsProcessingUploadFile() throws IOException {
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
        savedMedia.setChecksum("123");
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b952a79b6836213259aae1895_1_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/uploadFile/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b952a79b6836213259aae1895_1_uf.rsp";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryData);

        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_RESPONSE_PROCESSING_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertTrue(foundMedia.isResponseCleaned());
    }

    @Test
    void givenProcessResponseFilesFailsMediaChecksum() throws IOException {
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
        savedMedia.setChecksum("123");
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/uploadFile/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryData);

        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertTrue(foundMedia.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verify(armDataManagementApi).listResponseBlobs(hashcode);
        verify(armDataManagementApi).getBlobData(createRecordFilename);
        verify(armDataManagementApi).getBlobData(uploadFileFilename);
        verify(armDataManagementApi).deleteBlobData(inputUploadFilename);
        verify(armDataManagementApi).deleteBlobData(createRecordFilename);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void givenProcessResponseFilesFailsAnnotationChecksum() throws IOException {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        String testAnnotation = "TestAnnotation";
        AnnotationEntity annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(testUser, testAnnotation);

        when(userIdentity.getUserAccount()).thenReturn(testUser);
        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = OffsetDateTime.now();
        final String checksum = "123";
        AnnotationDocumentEntity annotationDocument = dartsDatabase.getAnnotationStub()
            .createAndSaveAnnotationDocumentEntityWith(annotation, fileName, fileType, fileSize,
                                                       testUser, uploadedDateTime, checksum
            );

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            annotationDocument,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), annotationDocument.getId());
        String inputUploadFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/uploadFile/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryData);

        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);

        armResponseFilesProcessor.processResponseFiles();

        ExternalObjectDirectoryEntity foundAnnotationEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId(), foundAnnotationEod.getStatus().getId());
        assertTrue(foundAnnotationEod.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verify(armDataManagementApi).listResponseBlobs(hashcode);
        verify(armDataManagementApi).getBlobData(createRecordFilename);
        verify(armDataManagementApi).getBlobData(uploadFileFilename);
        verify(armDataManagementApi).deleteBlobData(inputUploadFilename);
        verify(armDataManagementApi).deleteBlobData(createRecordFilename);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    @Disabled("Impacted by V1_364_*.sql")
    void givenProcessResponseFilesFailsTranscriptionChecksum() throws IOException {
        authorisationStub.givenTestSchema();
        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();

        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        final String checksum = "123";
        TranscriptionDocumentEntity transcriptionDocumentEntity = TranscriptionStub.createTranscriptionDocumentEntity(
            transcriptionEntity, fileName, fileType, fileSize, testUser, checksum);
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        dartsDatabase.getTranscriptionDocumentRepository().save(transcriptionDocumentEntity);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            transcriptionDocumentEntity,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setTransferAttempts(1);
        dartsDatabase.getExternalObjectDirectoryRepository().save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), transcriptionDocumentEntity.getId());
        String inputUploadFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/uploadFile/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryData);

        armResponseFilesProcessor.processResponseFiles();

        ExternalObjectDirectoryEntity foundTranscriptionEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertNotNull(foundTranscriptionEod);
        assertEquals(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId(), foundTranscriptionEod.getStatus().getId());
        assertTrue(foundTranscriptionEod.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verify(armDataManagementApi).listResponseBlobs(hashcode);
        verify(armDataManagementApi).getBlobData(createRecordFilename);
        verify(armDataManagementApi).getBlobData(uploadFileFilename);
        verify(armDataManagementApi).deleteBlobData(inputUploadFilename);
        verify(armDataManagementApi).deleteBlobData(createRecordFilename);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void givenProcessResponseFilesSuccessfullyCompletesForMedia() throws IOException {
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
        String checksum = "C3CCA7021CF79B42F245AF350601C284";
        savedMedia.setChecksum(checksum);
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );
        armEod.setChecksum(checksum);
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String createRecordTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/createRecord/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String createRecordFileJson = TestUtils.getContentsFromFile(createRecordTestFilename);
        BinaryData createRecordFileBinaryData = BinaryData.fromString(createRecordFileJson);
        when(armDataManagementApi.getBlobData(createRecordFilename)).thenReturn(createRecordFileBinaryData);

        String uploadFileTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/uploadFile/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryData);

        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(STORED.getId(), foundMedia.getStatus().getId());
        assertEquals("e7cde7c6-15d7-4c7e-a85d-a468c7ea72b9", foundMedia.getExternalFileId());
        assertEquals("1cf976c7-cedd-703f-ab70-01588bd56d50", foundMedia.getExternalRecordId());
        assertTrue(foundMedia.isResponseCleaned());

    }

    @Test
    void givenProcessResponseFilesSuccessfullyCompletesForAnnotation() throws IOException {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        String testAnnotation = "TestAnnotation";
        AnnotationEntity annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(testUser, testAnnotation);

        when(userIdentity.getUserAccount()).thenReturn(testUser);
        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = OffsetDateTime.now();
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        AnnotationDocumentEntity annotationDocument = dartsDatabase.getAnnotationStub()
            .createAndSaveAnnotationDocumentEntityWith(annotation, fileName, fileType, fileSize,
                                                       testUser, uploadedDateTime, checksum
            );

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            annotationDocument,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setChecksum(checksum);
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), annotationDocument.getId());
        String inputUploadFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/uploadFile/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryData);

        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);

        armResponseFilesProcessor.processResponseFiles();

        ExternalObjectDirectoryEntity foundAnnotationEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(STORED.getId(), foundAnnotationEod.getStatus().getId());
        assertEquals("e7cde7c6-15d7-4c7e-a85d-a468c7ea72b9", foundAnnotationEod.getExternalFileId());
        assertEquals("1cf976c7-cedd-703f-ab70-01588bd56d50", foundAnnotationEod.getExternalRecordId());
        assertTrue(foundAnnotationEod.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verify(armDataManagementApi).listResponseBlobs(hashcode);
        verify(armDataManagementApi).getBlobData(createRecordFilename);
        verify(armDataManagementApi).getBlobData(uploadFileFilename);
        verify(armDataManagementApi).deleteBlobData(inputUploadFilename);
        verify(armDataManagementApi).deleteBlobData(createRecordFilename);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    @Disabled("Impacted by V1_364_*.sql")
    void givenProcessResponseFilesSuccessfullyCompletesForTranscription() throws IOException {
        authorisationStub.givenTestSchema();
        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();

        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        TranscriptionDocumentEntity transcriptionDocumentEntity = TranscriptionStub.createTranscriptionDocumentEntity(
            transcriptionEntity, fileName, fileType, fileSize, testUser, checksum);
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        dartsDatabase.getTranscriptionDocumentRepository().save(transcriptionDocumentEntity);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            transcriptionDocumentEntity,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setChecksum(checksum);
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), transcriptionDocumentEntity.getId());
        String inputUploadFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String createRecordTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/createRecord/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String createRecordFileJson = TestUtils.getContentsFromFile(createRecordTestFilename);
        BinaryData createRecordBinaryData = BinaryData.fromString(createRecordFileJson);
        when(armDataManagementApi.getBlobData(createRecordFilename)).thenReturn(createRecordBinaryData);

        String uploadFileTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/uploadFile/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryData);

        armResponseFilesProcessor.processResponseFiles();

        ExternalObjectDirectoryEntity foundTranscriptionEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(STORED.getId(), foundTranscriptionEod.getStatus().getId());
        assertEquals("e7cde7c6-15d7-4c7e-a85d-a468c7ea72b9", foundTranscriptionEod.getExternalFileId());
        assertEquals("1cf976c7-cedd-703f-ab70-01588bd56d50", foundTranscriptionEod.getExternalRecordId());
        assertTrue(foundTranscriptionEod.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verify(armDataManagementApi).listResponseBlobs(hashcode);
        verify(armDataManagementApi).getBlobData(createRecordFilename);
        verify(armDataManagementApi).getBlobData(uploadFileFilename);
        verify(armDataManagementApi).deleteBlobData(inputUploadFilename);
        verify(armDataManagementApi).deleteBlobData(createRecordFilename);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename);
        verifyNoMoreInteractions(armDataManagementApi);
    }


    @Test
    void givenProcessResponseFilesFailsMediaChecksumNull() throws IOException {
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
        savedMedia.setChecksum(null);
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/uploadFile/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryData);

        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertTrue(foundMedia.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verify(armDataManagementApi).listResponseBlobs(hashcode);
        verify(armDataManagementApi).getBlobData(createRecordFilename);
        verify(armDataManagementApi).getBlobData(uploadFileFilename);
        verify(armDataManagementApi).deleteBlobData(inputUploadFilename);
        verify(armDataManagementApi).deleteBlobData(createRecordFilename);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void givenProcessResponseFilesFailsWithInvalidLines() throws IOException {
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
        savedMedia.setChecksum("C3CCA7021CF79B42F245AF350601C284");
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String invalidLineFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_0_il.rsp";
        String failedUploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_4117b202-91de-4530-9fc5-8328f25068ba_0_uf.rsp";
        hashcodeResponseBlobs.add(invalidLineFileFilename);
        hashcodeResponseBlobs.add(failedUploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/uploadFile/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(failedUploadFileFilename)).thenReturn(uploadFileBinaryData);

        String invalidLineTestFilename = "tests/arm/service/ArmResponseFilesProcessorTest/uploadFile/" +
            "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String invalidLineFileJson = TestUtils.getContentsFromFile(invalidLineTestFilename);
        BinaryData invalidLineBinaryData = BinaryData.fromString(invalidLineFileJson);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename)).thenReturn(invalidLineBinaryData);

        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(failedUploadFileFilename)).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_RESPONSE_MANIFEST_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(2, foundMedia.getTransferAttempts());
        assertTrue(foundMedia.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verify(armDataManagementApi).listResponseBlobs(hashcode);
        verify(armDataManagementApi).getBlobData(failedUploadFileFilename);
        verify(armDataManagementApi).getBlobData(invalidLineFileFilename);
        verify(armDataManagementApi).deleteBlobData(inputUploadFilename);
        verify(armDataManagementApi).deleteBlobData(failedUploadFileFilename);
        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void givenProcessResponseFilesFailsForInvalidLinesWithInvalidName() {
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
        savedMedia.setChecksum("C3CCA7021CF79B42F245AF350601C284");
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        String inputUploadFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> inputUploadFilenameResponseBlobs = new ArrayList<>();
        inputUploadFilenameResponseBlobs.add(inputUploadFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(inputUploadFilenameResponseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String invalidLineFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_il.rsp";
        String failedUploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_4117b202-91de-4530-9fc5-8328f25068ba_0_uf.rsp";
        hashcodeResponseBlobs.add(invalidLineFileFilename);
        hashcodeResponseBlobs.add(failedUploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(failedUploadFileFilename)).thenReturn(true);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        armResponseFilesProcessor.processResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_RESPONSE_PROCESSING_FAILED.getId(), foundMedia.getStatus().getId());
        assertTrue(foundMedia.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verify(armDataManagementApi).listResponseBlobs(hashcode);
        verify(armDataManagementApi).getBlobData(failedUploadFileFilename);
        verify(armDataManagementApi).getBlobData(invalidLineFileFilename);
        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename);
        verify(armDataManagementApi).deleteBlobData(failedUploadFileFilename);
        verify(armDataManagementApi).deleteBlobData(inputUploadFilename);
        verifyNoMoreInteractions(armDataManagementApi);
    }
}
