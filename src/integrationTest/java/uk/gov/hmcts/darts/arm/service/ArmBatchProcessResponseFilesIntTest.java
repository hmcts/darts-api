package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.arm.service.impl.ArmBatchProcessResponseFilesImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.MediaTestData;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;

@SuppressWarnings("VariableDeclarationUsageDistance")
class ArmBatchProcessResponseFilesIntTest extends IntegrationBase {

    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 6, 10);

    @Autowired
    private ArmResponseFilesProcessor armResponseFilesProcessor;

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Autowired
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Autowired
    private ExternalLocationTypeRepository externalLocationTypeRepository;


    @MockBean
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Autowired
    private ObjectMapper objectMapper;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @Autowired
    private AuthorisationStub authorisationStub;

    @TempDir
    private File tempDirectory;

    private ArmBatchProcessResponseFiles armBatchProcessResponseFiles;


    @BeforeEach
    void setupData() {

        armBatchProcessResponseFiles = new ArmBatchProcessResponseFilesImpl(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            armDataManagementApi,
            fileOperationService,
            armDataManagementConfiguration,
            objectMapper,
            userIdentity,
            currentTimeHelper,
            armResponseFilesProcessor
        );
    }

    @Test
    void batchProcessResponseFiles() throws IOException {

        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity media1 = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-06-10T13:00:00Z"),
                OffsetDateTime.parse("2023-06-10T13:45:00Z"),
                1
            ));

        MediaEntity media2 = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-06-10T13:00:00Z"),
                OffsetDateTime.parse("2023-06-10T13:45:00Z"),
                2
            ));

        MediaEntity media3 = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-06-10T13:00:00Z"),
                OffsetDateTime.parse("2023-06-10T13:45:00Z"),
                3
            ));

        MediaEntity media4 = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-06-10T14:00:00Z"),
                OffsetDateTime.parse("2023-06-10T14:45:00Z"),
                1
            ));

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifest2Uuid = UUID.randomUUID().toString();

        ExternalObjectDirectoryEntity armEod1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );
        armEod1.setTransferAttempts(1);
        armEod1.setManifestFile(manifest1Uuid);
        dartsDatabase.save(armEod1);

        ExternalObjectDirectoryEntity armEod2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media2,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );
        armEod2.setTransferAttempts(1);
        armEod2.setManifestFile(manifest1Uuid);
        dartsDatabase.save(armEod2);

        ExternalObjectDirectoryEntity armEod3 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media3,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );
        armEod3.setTransferAttempts(1);
        armEod3.setManifestFile(manifest1Uuid);
        dartsDatabase.save(armEod3);

        ExternalObjectDirectoryEntity armEod4 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media4,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );
        armEod4.setTransferAttempts(1);
        armEod4.setManifestFile(manifest2Uuid);
        dartsDatabase.save(armEod4);

        String prefix = "DARTS";
        List<String> responseBlobs = new ArrayList<>();
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        when(armDataManagementConfiguration.getBatchSize()).thenReturn(2);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn("DARTS");

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/submission/DARTS_%s.a360", manifest1Uuid);
        String blobNameAndPath2 = String.format("dropzone/DARTS/submission/DARTS_%s.a360", manifest2Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);
        blobNamesAndPaths.add(blobNameAndPath2);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        String continuationToken = null;
        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix, continuationToken)).thenReturn(continuationTokenBlobs);

        String createRecordFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", manifest1Uuid);
        String uploadFileFilename1 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp", manifest1Uuid);
        String createRecordFilename2 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_1_cr.rsp", manifest1Uuid);
        String invalidLineFileFilename2 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_0_il.rsp", manifest1Uuid);
        String uploadFileFilename3 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1897_1_uf.rsp", manifest1Uuid);
        String invalidLineFileFilename3 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1897_0_il.rsp", manifest1Uuid);
        List<String> hashcodeResponses = new ArrayList<>();
        hashcodeResponses.add(createRecordFilename1);
        hashcodeResponses.add(uploadFileFilename1);
        hashcodeResponses.add(createRecordFilename2);
        hashcodeResponses.add(invalidLineFileFilename2);
        hashcodeResponses.add(uploadFileFilename3);
        hashcodeResponses.add(invalidLineFileFilename3);

        when(armDataManagementApi.listResponseBlobs(manifest1Uuid)).thenReturn(hashcodeResponses);

        String validUploadFile1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/validResponses/validUfFile.a360";

        BinaryData uploadFileBinaryData = convertStringToBinaryData(getUploadFileContents(validUploadFile1, armEod1.getId(), media1.getChecksum()));
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryData);
        /*when(armDataManagementApi.getBlobData(invalidLineFileFilename2)).thenReturn();
        when(armDataManagementApi.getBlobData(uploadFileFilename3)).thenReturn();
        when(armDataManagementApi.getBlobData(invalidLineFileFilename3)).thenReturn();*/

        armBatchProcessResponseFiles.batchProcessResponseFiles();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());

    }

    private BinaryData convertStringToBinaryData(String contents) {
        return BinaryData.fromString(contents);
    }

    private String getUploadFileContents(String filename, int externalObjectDirectoryId, String checksum) throws IOException {
        String expectedResponse = getContentsFromFile(filename);
        expectedResponse = expectedResponse.replaceAll("<CHECKSUM>", checksum);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(externalObjectDirectoryId));
        return expectedResponse;
    }
}
