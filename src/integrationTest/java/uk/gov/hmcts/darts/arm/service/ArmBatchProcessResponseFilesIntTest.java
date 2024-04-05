package uk.gov.hmcts.darts.arm.service;

import com.azure.core.exception.AzureException;
import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
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
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.MediaTestData;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;

@SuppressWarnings({"VariableDeclarationUsageDistance", "PMD.NcssCount", "ExcessiveImports"})
class ArmBatchProcessResponseFilesIntTest extends IntegrationBase {

    private static final String PREFIX = "DARTS";
    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 6, 10);
    public static final String T_13_00_00_Z = "2023-06-10T13:00:00Z";
    public static final String T_13_45_00_Z = "2023-06-10T13:45:00Z";

    @Autowired
    private ArmResponseFilesProcessor armResponseFilesProcessor;

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Autowired
    private ExternalObjectDirectoryService externalObjectDirectoryService;
    @Autowired
    private MediaRepository mediaRepository;
    @Autowired
    private TranscriptionDocumentRepository transcriptionDocumentRepository;
    @Autowired
    private AnnotationDocumentRepository annotationDocumentRepository;
    @Autowired
    private CaseDocumentRepository caseDocumentRepository;

    @MockBean
    private ArmDataManagementApi armDataManagementApi;
    @Autowired
    private FileOperationService fileOperationService;
    @MockBean
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private UserIdentity userIdentity;
    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @Autowired
    private AuthorisationStub authorisationStub;

    @TempDir
    private File tempDirectory;

    private ArmBatchProcessResponseFiles armBatchProcessResponseFiles;
    private String continuationToken;


    @BeforeEach
    void setupData() {

        armBatchProcessResponseFiles = new ArmBatchProcessResponseFilesImpl(
            externalObjectDirectoryRepository,
            armDataManagementApi,
            fileOperationService,
            armDataManagementConfiguration,
            objectMapper,
            userIdentity,
            currentTimeHelper,
            armResponseFilesProcessor,
            externalObjectDirectoryService,
            mediaRepository,
            transcriptionDocumentRepository,
            annotationDocumentRepository,
            caseDocumentRepository
        );

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

    }

    @Test
    void batchProcessResponseFiles_WithMediaReturnsSuccess() throws IOException {

        // given
        HearingEntity hearing = dartsDatabase.createHearing("NEWCASTLE", "Int Test Courtroom 2", "2", HEARING_DATE);

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);

        MediaEntity media2 = createMediaEntity(hearing, startTime, endTime, 2);

        MediaEntity media3 = createMediaEntity(hearing, startTime, endTime, 3);

        MediaEntity media4 = createMediaEntity(hearing, startTime, endTime, 4);

        OffsetDateTime startTime2 = OffsetDateTime.parse("2023-06-10T14:00:00Z");
        OffsetDateTime endTime2 = OffsetDateTime.parse("2023-06-10T14:45:00Z");
        MediaEntity media5 = createMediaEntity(hearing, startTime2, endTime2, 1);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifest2Uuid = UUID.randomUUID().toString();

        String manifestFile1 = "DARTS_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod1.setTransferAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod1);

        ExternalObjectDirectoryEntity armEod2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media2, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod2.setTransferAttempts(1);
        armEod2.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod2);

        ExternalObjectDirectoryEntity armEod3 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media3, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod3.setTransferAttempts(1);
        armEod3.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod3);

        ExternalObjectDirectoryEntity armEod4 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media4, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod4.setTransferAttempts(1);
        armEod4.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod4);

        String manifestFile2 = "DARTS_" + manifest2Uuid + ".a360";
        ExternalObjectDirectoryEntity armEod5 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media5, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod5.setTransferAttempts(1);
        armEod5.setManifestFile(manifestFile2);
        dartsDatabase.save(armEod5);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DARTS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        String blobNameAndPath2 = String.format("dropzone/DARTS/response/DARTS_%s_7a374f19a9ce7dc9cc480ea8d4eca0fc_1_iu.rsp", manifest2Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);
        blobNamesAndPaths.add(blobNameAndPath2);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(PREFIX, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp", hashcode1);
        String createRecordFilename2 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_1_cr.rsp", hashcode1);
        String invalidLineFileFilename2 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_0_il.rsp", hashcode1);
        String uploadFileFilename3 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1897_1_uf.rsp", hashcode1);
        String invalidLineFileFilename3 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1897_0_il.rsp", hashcode1);
        List<String> hashcodeResponses = List.of(createRecordFilename1, uploadFileFilename1,
                                                 createRecordFilename2, invalidLineFileFilename2,
                                                 uploadFileFilename3, invalidLineFileFilename3);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String validUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
        String createRecordFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String invalidLineFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";
        String validUploadFileTest3 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
        String invalidLineFileTest3 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";

        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, armEod1.getId()));
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(getUploadFileContents(validUploadFileTest1, armEod1.getId(), media1.getChecksum()));
        BinaryData createRecordBinaryDataTest2 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest2, armEod2.getId()));
        BinaryData invalidLineFileBinaryDataTest2 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest2, armEod2.getId()));
        BinaryData uploadFileBinaryDataTest3 = convertStringToBinaryData(getUploadFileContents(validUploadFileTest3, armEod3.getId(), media3.getChecksum()));
        BinaryData invalidLineFileBinaryDataTest3 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest3, armEod3.getId()));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);
        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenReturn(createRecordBinaryDataTest2);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename2)).thenReturn(invalidLineFileBinaryDataTest2);
        when(armDataManagementApi.getBlobData(uploadFileFilename3)).thenReturn(uploadFileBinaryDataTest3);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename3)).thenReturn(invalidLineFileBinaryDataTest3);

        String hashcode2 = "7a374f19a9ce7dc9cc480ea8d4eca0fc";
        String createRecordFilename4 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1890_1_cr.rsp", hashcode2);
        List<String> hashcodeResponses2 = List.of(createRecordFilename4);
        when(armDataManagementApi.listResponseBlobs(hashcode2)).thenReturn(hashcodeResponses2);
        String createRecordFileTest4 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        BinaryData createRecordBinaryDataTest4 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest4, armEod4.getId()));
        when(armDataManagementApi.getBlobData(createRecordFilename4)).thenReturn(createRecordBinaryDataTest4);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename3)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename3)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(endTime2);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(STORED.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertTrue(foundMedia.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList2 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media2, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList2.size());
        ExternalObjectDirectoryEntity foundMedia2 = foundMediaList2.get(0);
        assertEquals(ARM_RESPONSE_MANIFEST_FAILED.getId(), foundMedia2.getStatus().getId());
        assertEquals(1, foundMedia2.getVerificationAttempts());
        assertTrue(foundMedia2.isResponseCleaned());


        List<ExternalObjectDirectoryEntity> foundMediaList3 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media3, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList3.size());
        ExternalObjectDirectoryEntity foundMedia3 = foundMediaList3.get(0);
        assertEquals(ARM_RESPONSE_MANIFEST_FAILED.getId(), foundMedia3.getStatus().getId());
        assertEquals(1, foundMedia3.getVerificationAttempts());
        assertTrue(foundMedia3.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList4 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media4, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList4.size());
        ExternalObjectDirectoryEntity foundMedia4 = foundMediaList4.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia4.getStatus().getId());
        assertEquals(1, foundMedia4.getVerificationAttempts());
        assertFalse(foundMedia4.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList5 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media5, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList5.size());
        ExternalObjectDirectoryEntity foundMedia5 = foundMediaList5.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia5.getStatus().getId());
        assertEquals(1, foundMedia5.getVerificationAttempts());
        assertFalse(foundMedia5.isResponseCleaned());
    }

    @Test
    void batchProcessResponseFiles_WithInvalidJson() throws IOException {

        // given
        HearingEntity hearing = dartsDatabase.createHearing("NEWCASTLE", "Int Test Courtroom 2", "2", HEARING_DATE);

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);

        MediaEntity media2 = createMediaEntity(hearing, startTime, endTime, 2);

        MediaEntity media3 = createMediaEntity(hearing, startTime, endTime, 3);

        MediaEntity media4 = createMediaEntity(hearing, startTime, endTime, 4);

        OffsetDateTime startTime2 = OffsetDateTime.parse("2023-06-10T14:00:00Z");
        OffsetDateTime endTime2 = OffsetDateTime.parse("2023-06-10T14:45:00Z");
        MediaEntity media5 = createMediaEntity(hearing, startTime2, endTime2, 1);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifest2Uuid = UUID.randomUUID().toString();

        String manifestFile1 = "DARTS_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod1.setTransferAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod1);

        ExternalObjectDirectoryEntity armEod2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media2, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod2.setTransferAttempts(1);
        armEod2.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod2);

        ExternalObjectDirectoryEntity armEod3 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media3, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod3.setTransferAttempts(1);
        armEod3.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod3);

        ExternalObjectDirectoryEntity armEod4 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media4, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod4.setTransferAttempts(1);
        armEod4.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod4);

        String manifestFile2 = "DARTS_" + manifest2Uuid + ".a360";
        ExternalObjectDirectoryEntity armEod5 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media5, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod5.setTransferAttempts(1);
        armEod5.setManifestFile(manifestFile2);
        dartsDatabase.save(armEod5);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DARTS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        String blobNameAndPath2 = String.format("dropzone/DARTS/response/DARTS_%s_7a374f19a9ce7dc9cc480ea8d4eca0fc_1_iu.rsp", manifest2Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);
        blobNamesAndPaths.add(blobNameAndPath2);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(PREFIX, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp", hashcode1);
        String createRecordFilename2 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_1_cr.rsp", hashcode1);
        String invalidLineFileFilename2 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_0_il.rsp", hashcode1);
        String uploadFileFilename3 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1897_1_uf.rsp", hashcode1);
        String invalidLineFileFilename3 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1897_0_il.rsp", hashcode1);
        List<String> hashcodeResponses = List.of(createRecordFilename1, uploadFileFilename1,
                                                 createRecordFilename2, invalidLineFileFilename2,
                                                 uploadFileFilename3, invalidLineFileFilename3);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/InvalidResponses/CreateRecord.rsp";
        String validUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/InvalidResponses/UploadFile.rsp";
        String createRecordFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/InvalidResponses/CreateRecord.rsp";
        String invalidLineFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/InvalidResponses/InvalidLineFile.rsp";
        String validUploadFileTest3 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/InvalidResponses/UploadFile.rsp";
        String invalidLineFileTest3 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/InvalidResponses/InvalidLineFile.rsp";

        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, armEod1.getId()));
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(getUploadFileContents(validUploadFileTest1, armEod1.getId(), media1.getChecksum()));
        BinaryData createRecordBinaryDataTest2 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest2, armEod2.getId()));
        BinaryData invalidLineFileBinaryDataTest2 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest2, armEod2.getId()));
        BinaryData uploadFileBinaryDataTest3 = convertStringToBinaryData(getUploadFileContents(validUploadFileTest3, armEod3.getId(), media3.getChecksum()));
        BinaryData invalidLineFileBinaryDataTest3 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest3, armEod3.getId()));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);
        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenReturn(createRecordBinaryDataTest2);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename2)).thenReturn(invalidLineFileBinaryDataTest2);
        when(armDataManagementApi.getBlobData(uploadFileFilename3)).thenReturn(uploadFileBinaryDataTest3);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename3)).thenReturn(invalidLineFileBinaryDataTest3);

        String hashcode2 = "7a374f19a9ce7dc9cc480ea8d4eca0fc";
        String createRecordFilename4 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1890_1_cr.rsp", hashcode2);
        List<String> hashcodeResponses2 = List.of(createRecordFilename4);
        when(armDataManagementApi.listResponseBlobs(hashcode2)).thenReturn(hashcodeResponses2);
        String createRecordFileTest4 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        BinaryData createRecordBinaryDataTest4 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest4, armEod4.getId()));
        when(armDataManagementApi.getBlobData(createRecordFilename4)).thenReturn(createRecordBinaryDataTest4);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename3)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename3)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(endTime2);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList2 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media2, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList2.size());
        ExternalObjectDirectoryEntity foundMedia2 = foundMediaList2.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia2.getStatus().getId());
        assertEquals(1, foundMedia2.getVerificationAttempts());
        assertFalse(foundMedia2.isResponseCleaned());


        List<ExternalObjectDirectoryEntity> foundMediaList3 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media3, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList3.size());
        ExternalObjectDirectoryEntity foundMedia3 = foundMediaList3.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia3.getStatus().getId());
        assertEquals(1, foundMedia3.getVerificationAttempts());
        assertFalse(foundMedia3.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList4 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media4, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList4.size());
        ExternalObjectDirectoryEntity foundMedia4 = foundMediaList4.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia4.getStatus().getId());
        assertEquals(1, foundMedia4.getVerificationAttempts());
        assertFalse(foundMedia4.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList5 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media5, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList5.size());
        ExternalObjectDirectoryEntity foundMedia5 = foundMediaList5.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia5.getStatus().getId());
        assertEquals(1, foundMedia5.getVerificationAttempts());
        assertFalse(foundMedia5.isResponseCleaned());
    }

    @Test
    void batchProcessResponseFiles_WithInvalidFilenameStatus() throws IOException {

        // given
        HearingEntity hearing = dartsDatabase.createHearing("NEWCASTLE", "Int Test Courtroom 2", "2", HEARING_DATE);

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);

        MediaEntity media2 = createMediaEntity(hearing, startTime, endTime, 2);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = "DARTS_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod1.setTransferAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod1);

        ExternalObjectDirectoryEntity armEod2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media2, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod2.setTransferAttempts(1);
        armEod2.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod2);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DARTS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(PREFIX, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1895_0_uf.rsp", hashcode1);
        String createRecordFilename2 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_0_cr.rsp", hashcode1);
        String invalidLineFileFilename2 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_1_il.rsp", hashcode1);

        List<String> hashcodeResponses = List.of(createRecordFilename1, uploadFileFilename1,
                                                 createRecordFilename2, invalidLineFileFilename2);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String validUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
        String createRecordFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String invalidLineFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";

        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, armEod1.getId()));
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(getUploadFileContents(validUploadFileTest1, armEod1.getId(), media1.getChecksum()));
        BinaryData createRecordBinaryDataTest2 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest2, armEod2.getId()));
        BinaryData invalidLineFileBinaryDataTest2 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest2, armEod2.getId()));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);
        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenReturn(createRecordBinaryDataTest2);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename2)).thenReturn(invalidLineFileBinaryDataTest2);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename2)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(endTime);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_RESPONSE_PROCESSING_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertTrue(foundMedia.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList2 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media2, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList2.size());
        ExternalObjectDirectoryEntity foundMedia2 = foundMediaList2.get(0);
        assertEquals(ARM_RESPONSE_PROCESSING_FAILED.getId(), foundMedia2.getStatus().getId());
        assertEquals(1, foundMedia2.getVerificationAttempts());
        assertTrue(foundMedia2.isResponseCleaned());

    }

    @Test
    void batchProcessResponseFiles_WithTranscriptionReturnsSuccess() throws IOException {

        // given
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

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = "DARTS_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            transcriptionDocumentEntity,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setTransferAttempts(1);
        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DARTS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(PREFIX, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp", hashcode1);
        List<String> hashcodeResponses = getHashcodeResponses(hashcode1, createRecordFilename1, uploadFileFilename1);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String validUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";

        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, armEod.getId()));
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(
            getUploadFileContents(validUploadFileTest1, armEod.getId(), transcriptionDocumentEntity.getChecksum()));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(transcriptionEntity.getEndTime());

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(PREFIX);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        ExternalObjectDirectoryEntity foundTranscriptionEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(STORED.getId(), foundTranscriptionEod.getStatus().getId());
        assertEquals("A360230516_TestIngestion_1.docx", foundTranscriptionEod.getExternalFileId());
        assertEquals(String.valueOf(armEod.getId()), foundTranscriptionEod.getExternalRecordId());
        assertTrue(foundTranscriptionEod.isResponseCleaned());
    }

    @NotNull
    private static List<String> getHashcodeResponses(String hashcode1, String createRecordFilename1, String uploadFileFilename1) {
        String createRecordFilename2 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_1_cr.rsp", hashcode1);
        String invalidLineFileFilename2 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_0_il.rsp", hashcode1);
        String uploadFileFilename3 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1897_1_uf.rsp", hashcode1);
        String invalidLineFileFilename3 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1897_0_il.rsp", hashcode1);
        return List.of(createRecordFilename1, uploadFileFilename1,
                       createRecordFilename2, invalidLineFileFilename2,
                       uploadFileFilename3, invalidLineFileFilename3);

    }

    @Test
    void batchProcessResponseFiles_WithInvalidTranscriptionChecksum() throws IOException {

        // given
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

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = "DARTS_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            transcriptionDocumentEntity,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setTransferAttempts(1);
        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DARTS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(PREFIX, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp", hashcode1);
        List<String> hashcodeResponses = getHashcodeResponses(hashcode1, createRecordFilename1, uploadFileFilename1);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String validUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";

        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, armEod.getId()));
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(
            getUploadFileContents(validUploadFileTest1, armEod.getId(), "1234"));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(transcriptionEntity.getEndTime());

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(PREFIX);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        ExternalObjectDirectoryEntity foundTranscriptionEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId(), foundTranscriptionEod.getStatus().getId());
        assertTrue(foundTranscriptionEod.isResponseCleaned());
    }

    @Test
    void batchProcessResponseFiles_WithAnnotationReturnsSuccess() throws IOException {

        // given
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

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = "DARTS_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            annotationDocument,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setTransferAttempts(1);
        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DARTS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(PREFIX, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp", hashcode1);
        List<String> hashcodeResponses = getHashcodeResponses(hashcode1, createRecordFilename1, uploadFileFilename1);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String validUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";

        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, armEod.getId()));
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(
            getUploadFileContents(validUploadFileTest1, armEod.getId(), annotationDocument.getChecksum()));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(annotationDocument.getUploadedDateTime());

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(PREFIX);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        ExternalObjectDirectoryEntity foundAnnotationEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(STORED.getId(), foundAnnotationEod.getStatus().getId());
        assertEquals("A360230516_TestIngestion_1.docx", foundAnnotationEod.getExternalFileId());
        assertEquals(String.valueOf(armEod.getId()), foundAnnotationEod.getExternalRecordId());
        assertTrue(foundAnnotationEod.isResponseCleaned());
    }

    @Test
    void batchProcessResponseFiles_WithCaseDocumentReturnsSuccess() throws IOException {

        // given
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntity, uploadedBy);
        caseDocument.setFileName("test_case_document.docx");
        dartsDatabase.save(caseDocument);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = "DARTS_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            caseDocument,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setTransferAttempts(1);
        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DARTS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(PREFIX, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp", hashcode1);
        List<String> hashcodeResponses = getHashcodeResponses(hashcode1, createRecordFilename1, uploadFileFilename1);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String validUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";

        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, armEod.getId()));
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(
            getUploadFileContents(validUploadFileTest1, armEod.getId(), caseDocument.getChecksum()));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(caseDocument.getCreatedTs());

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(PREFIX);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        ExternalObjectDirectoryEntity foundAnnotationEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(STORED.getId(), foundAnnotationEod.getStatus().getId());
        assertEquals("A360230516_TestIngestion_1.docx", foundAnnotationEod.getExternalFileId());
        assertEquals(String.valueOf(armEod.getId()), foundAnnotationEod.getExternalRecordId());
        assertTrue(foundAnnotationEod.isResponseCleaned());
    }

    @Test
    void batchProcessResponseFiles_WithCaseDocumentInvalidResponseFilenames() throws IOException {

        // given
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntity, uploadedBy);
        caseDocument.setFileName("test_case_document.docx");
        dartsDatabase.save(caseDocument);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = "DARTS_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            caseDocument,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setTransferAttempts(1);
        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DARTS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(PREFIX, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_invalid_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1895_invalid_1_uf.rsp", hashcode1);
        String createRecordFilename2 = String.format("%s__a17b9015-e6ad-77c5-8d1e-13259aae1896_cr.rsp", hashcode1);
        String invalidLineFileFilename2 = String.format("%s__a17b9015-e6ad-77c5-8d1e-13259aae1896_il.rsp", hashcode1);
        String uploadFileFilename3 = String.format("%s_1_uf.rsp", hashcode1);
        String invalidLineFileFilename3 = String.format("%s_0_il.rsp", hashcode1);
        List<String> hashcodeResponses = List.of(createRecordFilename1, uploadFileFilename1,
                                                 createRecordFilename2, invalidLineFileFilename2,
                                                 uploadFileFilename3, invalidLineFileFilename3);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String validUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
        String createRecordFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String invalidLineFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";
        String validUploadFileTest3 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
        String invalidLineFileTest3 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";

        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, armEod.getId()));
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(
            getUploadFileContents(validUploadFileTest1, armEod.getId(), caseDocument.getChecksum()));
        BinaryData createRecordBinaryDataTest2 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest2, 1234));
        BinaryData invalidLineFileBinaryDataTest2 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest2, 234));
        BinaryData uploadFileBinaryDataTest3 = convertStringToBinaryData(getUploadFileContents(validUploadFileTest3, 2233, "123"));
        BinaryData invalidLineFileBinaryDataTest3 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest3, 7788));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);
        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenReturn(createRecordBinaryDataTest2);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename2)).thenReturn(invalidLineFileBinaryDataTest2);
        when(armDataManagementApi.getBlobData(uploadFileFilename3)).thenReturn(uploadFileBinaryDataTest3);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename3)).thenReturn(invalidLineFileBinaryDataTest3);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(caseDocument.getCreatedTs());

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(PREFIX);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        ExternalObjectDirectoryEntity foundAnnotationEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(ARM_DROP_ZONE.getId(), foundAnnotationEod.getStatus().getId());
        assertFalse(foundAnnotationEod.isResponseCleaned());
    }

    @Test
    void batchProcessResponseFiles_ReturnsNoRecordsToProcess() {

        // given
        HearingEntity hearing = dartsDatabase.createHearing("NEWCASTLE", "Int Test Courtroom 2", "2", HEARING_DATE);

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);

        String manifest1Uuid = UUID.randomUUID().toString();

        String manifestFile1 = "DARTS_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod1.setTransferAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod1);

        List<String> blobNamesAndPaths = new ArrayList<>();
        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(PREFIX, continuationToken)).thenReturn(continuationTokenBlobs);

        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(PREFIX);

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(PREFIX, continuationToken);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void batchProcessResponseFiles_ThrowsExceptionWhenListingPrefix() {

        // given
        HearingEntity hearing = dartsDatabase.createHearing("NEWCASTLE", "Int Test Courtroom 2", "2", HEARING_DATE);

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);

        String manifest1Uuid = UUID.randomUUID().toString();

        String manifestFile1 = "DARTS_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1, ARM_DROP_ZONE, ARM, UUID.randomUUID());
        armEod1.setTransferAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod1);

        when(armDataManagementApi.listResponseBlobsUsingMarker(PREFIX, continuationToken)).thenThrow(new AzureException());

        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(PREFIX);

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(PREFIX, continuationToken);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void batchProcessResponseFiles_ForBatchSizeZero() throws IOException {
        // given
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
        String uploadFileJson = getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryData);

        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);

        when(armDataManagementConfiguration.getBatchSize()).thenReturn(0);

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        ExternalObjectDirectoryEntity foundAnnotationEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(STORED.getId(), foundAnnotationEod.getStatus().getId());
        assertEquals("A360230516_TestIngestion_1.docx", foundAnnotationEod.getExternalFileId());
        assertEquals("152821", foundAnnotationEod.getExternalRecordId());
        assertTrue(foundAnnotationEod.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verify(armDataManagementApi).listResponseBlobs(hashcode);
        verify(armDataManagementApi).getBlobData("6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp");
        verify(armDataManagementApi).deleteBlobData(inputUploadFilename);
        verify(armDataManagementApi).deleteBlobData(createRecordFilename);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename);
        verifyNoMoreInteractions(armDataManagementApi);

    }

    @Test
    void batchProcessResponseFiles_ForBatchSizeMinusOne() {
        // given
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(-1);

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        verifyNoMoreInteractions(armDataManagementApi);

    }

    private MediaEntity createMediaEntity(HearingEntity hearing, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                startTime,
                endTime,
                channel
            ));

    }

    private BinaryData convertStringToBinaryData(String contents) {
        return BinaryData.fromString(contents);
    }

    private String getInvalidLineFileContents(String invalidLineFilename, Integer externalObjectDirectoryId) throws IOException {
        String expectedResponse = getContentsFromFile(invalidLineFilename);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(externalObjectDirectoryId));
        return expectedResponse;
    }


    private String getUploadFileContents(String uploadFilename, int externalObjectDirectoryId, String checksum) throws IOException {
        String expectedResponse = getContentsFromFile(uploadFilename);
        expectedResponse = expectedResponse.replaceAll("<CHECKSUM>", checksum);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(externalObjectDirectoryId));
        return expectedResponse;
    }

    private String getCreateRecordFileContents(String createRecordFilename, Integer externalObjectDirectoryId) throws IOException {
        String expectedResponse = getContentsFromFile(createRecordFilename);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(externalObjectDirectoryId));
        return expectedResponse;
    }
}
