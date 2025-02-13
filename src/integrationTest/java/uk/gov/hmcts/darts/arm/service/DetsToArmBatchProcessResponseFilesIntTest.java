package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.arm.service.impl.DetsToArmBatchProcessResponseFilesImpl;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.DETS;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MISSING_RESPONSE;

@SuppressWarnings({"VariableDeclarationUsageDistance", "PMD.NcssCount"})
class DetsToArmBatchProcessResponseFilesIntTest extends AbstractArmBatchProcessResponseFilesIntTest {

    @Autowired
    private DetsDataManagementConfiguration detsDataManagementConfiguration;
    @Autowired
    private ObjectStateRecordRepository osrRepository;

    @BeforeEach
    void setupData() {
        armBatchProcessResponseFiles = new DetsToArmBatchProcessResponseFilesImpl(
            externalObjectDirectoryRepository,
            armDataManagementApi,
            fileOperationService,
            armDataManagementConfiguration,
            objectMapper,
            userIdentity,
            currentTimeHelper,
            externalObjectDirectoryService,
            logApi,
            deleteArmResponseFilesHelper,
            detsDataManagementConfiguration,
            osrRepository
        );
    }

    @Override
    protected String prefix() {
        return "DETS";
    }

    // the tests below are a copy of some of the ones in the abstract class, with extra verifications added for the DETS specific behaviour

    @SneakyThrows
    @Test
    void batchProcessResponseFiles_WithMediaReturnsSuccess_UpdateObjectStateRecords() {
        // given
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimal();

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);

        MediaEntity media2 = createMediaEntity(hearing, startTime, endTime, 2);

        MediaEntity media3 = createMediaEntity(hearing, startTime, endTime, 3);

        OffsetDateTime startTime2 = OffsetDateTime.parse("2023-06-10T14:00:00Z");
        OffsetDateTime endTime2 = OffsetDateTime.parse("2023-06-10T14:45:00Z");
        MediaEntity media5 = createMediaEntity(hearing, startTime2, endTime2, 1);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifest2Uuid = UUID.randomUUID().toString();

        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID()).build();
        armEod1.setTransferAttempts(1);
        armEod1.setVerificationAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        armEod1.setChecksum("7017013d05bcc5032e142049081821d6");
        armEod1 = dartsPersistence.save(armEod1);

        ExternalObjectDirectoryEntity detsEod1 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(DETS)).externalLocation(UUID.randomUUID()).build();
        dartsPersistence.save(detsEod1);

        ObjectStateRecordEntity osr1 = new ObjectStateRecordEntity();
        osr1.setUuid(1L);
        osr1.setArmEodId(String.valueOf(armEod1.getId()));
        osrRepository.save(osr1);

        ExternalObjectDirectoryEntity armEod2 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media2).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID()).build();
        armEod2.setTransferAttempts(1);
        armEod2.setManifestFile(manifestFile1);
        armEod2.setChecksum("7017013d05bcc5032e142049081821d6");
        armEod2.setVerificationAttempts(1);
        armEod2 = dartsPersistence.save(armEod2);

        ExternalObjectDirectoryEntity detsEod2 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media2).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(DETS)).externalLocation(UUID.randomUUID()).build();
        detsEod2 = dartsPersistence.save(detsEod2);

        ObjectStateRecordEntity osr2 = new ObjectStateRecordEntity();
        osr2.setUuid(2L);
        osr2.setArmEodId(String.valueOf(armEod2.getId()));
        osrRepository.save(osr2);

        ExternalObjectDirectoryEntity armEod3 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media3).status(dartsDatabase
                                      .getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID()).build();
        armEod3.setTransferAttempts(1);
        armEod3.setManifestFile(manifestFile1);
        armEod3.setVerificationAttempts(1);
        armEod3 = dartsPersistence.save(armEod3);

        ObjectStateRecordEntity osr3 = new ObjectStateRecordEntity();
        osr3.setUuid(3L);
        osr3.setArmEodId(String.valueOf(armEod3.getId()));
        osrRepository.save(osr3);

        String manifestFile2 = prefix() + "_" + manifest2Uuid + ".a360";
        ExternalObjectDirectoryEntity armEod5 = PersistableFactory
            .getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media5).status(dartsDatabase
                                      .getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase
                                      .getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID()).build();
        armEod5.setTransferAttempts(1);
        armEod5.setManifestFile(manifestFile2);
        armEod5.setVerificationAttempts(1);
        armEod5 = dartsPersistence.save(armEod5);

        ObjectStateRecordEntity osr5 = new ObjectStateRecordEntity();
        osr5.setUuid(5L);
        osr5.setArmEodId(String.valueOf(armEod5.getId()));
        osrRepository.save(osr5);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DETS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        String blobNameAndPath2 = String.format("dropzone/DARTS/response/DETS_%s_7a374f19a9ce7dc9cc480ea8d4eca0fc_1_iu.rsp", manifest2Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);
        blobNamesAndPaths.add(blobNameAndPath2);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
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
        String createRecordFilename5 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1890_1_cr.rsp", hashcode2);
        List<String> hashcodeResponses2 = List.of(createRecordFilename5);
        when(armDataManagementApi.listResponseBlobs(hashcode2)).thenReturn(hashcodeResponses2);
        String createRecordFileTest5 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        BinaryData createRecordBinaryDataTest5 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest5, armEod5.getId()));
        when(armDataManagementApi.getBlobData(createRecordFilename5)).thenReturn(createRecordBinaryDataTest5);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename3)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename3)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(blobNameAndPath1)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(endTime2);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE);

        // then
        ObjectStateRecordEntity dbOsr1 = osrRepository.findByArmEodId(String.valueOf(armEod1.getId())).orElseThrow();
        assertThat(dbOsr1.getFlagRspnRecvdFromArml()).isTrue();
        assertThat(dbOsr1.getDateRspnRecvdFromArml()).isEqualTo(endTime2);
        assertThat(dbOsr1.getFlagFileIngestStatus()).isTrue();
        assertThat(dbOsr1.getDateFileIngestToArm()).isEqualTo(endTime2);
        assertThat(dbOsr1.getMd5FileIngestToArm()).isEqualTo("7017013d05bcc5032e142049081821d6");
        assertThat(dbOsr1.getIdResponseFile()).isEqualTo(
            String.format("DETS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid));
        assertThat(dbOsr1.getIdResponseCrFile()).isEqualTo("6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp");
        assertThat(dbOsr1.getIdResponseUfFile()).isEqualTo("6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp");
        assertThat(dbOsr1.getObjectStatus()).isNull();
        assertThat(dbOsr1.getFileSizeIngestToArm()).isEqualTo(11997);

        ObjectStateRecordEntity dbOsr2 = osrRepository.findByArmEodId(String.valueOf(armEod2.getId())).orElseThrow();
        assertThat(dbOsr2.getFlagRspnRecvdFromArml()).isTrue();
        assertThat(dbOsr2.getDateRspnRecvdFromArml()).isEqualTo(endTime2);
        assertThat(dbOsr2.getFlagFileIngestStatus()).isFalse();
        assertThat(dbOsr2.getDateFileIngestToArm()).isEqualTo(endTime2);
        assertThat(dbOsr2.getObjectStatus()).isEqualTo("PS.20023:INVALID_PARAMETERS:Invalid line: invalid json");
        assertThat(dbOsr2.getMd5FileIngestToArm()).isNull();
        assertThat(dbOsr2.getIdResponseFile()).isNull();
        assertThat(dbOsr2.getIdResponseCrFile()).isNull();
        assertThat(dbOsr2.getIdResponseUfFile()).isNull();
        assertThat(dbOsr2.getFlagFileDetsCleanupStatus()).isNull();
        assertThat(dbOsr2.getDateFileDetsCleanup()).isNull();
        assertThat(externalObjectDirectoryRepository.findById(detsEod2.getId())).isPresent();

        ObjectStateRecordEntity dbOsr3 = osrRepository.findByArmEodId(String.valueOf(armEod3.getId())).orElseThrow();
        assertThat(dbOsr3.getFlagRspnRecvdFromArml()).isTrue();
        assertThat(dbOsr3.getDateRspnRecvdFromArml()).isEqualTo(endTime2);
        assertThat(dbOsr3.getFlagFileIngestStatus()).isFalse();
        assertThat(dbOsr3.getDateFileIngestToArm()).isEqualTo(endTime2);
        assertThat(dbOsr3.getObjectStatus()).isEqualTo("PS.20023:INVALID_PARAMETERS:Invalid line: invalid json");
        assertThat(dbOsr3.getMd5FileIngestToArm()).isNull();
        assertThat(dbOsr3.getIdResponseFile()).isNull();
        assertThat(dbOsr3.getIdResponseCrFile()).isNull();
        assertThat(dbOsr3.getIdResponseUfFile()).isNull();
        assertThat(dbOsr3.getFlagFileDetsCleanupStatus()).isNull();
        assertThat(dbOsr3.getDateFileDetsCleanup()).isNull();

        ObjectStateRecordEntity dbOsr5 = osrRepository.findByArmEodId(String.valueOf(armEod5.getId())).orElseThrow();
        assertThat(dbOsr5.getFlagRspnRecvdFromArml()).isNull();
        assertThat(dbOsr5.getDateRspnRecvdFromArml()).isNull();
        assertThat(dbOsr5.getFlagFileIngestStatus()).isNull();
        assertThat(dbOsr5.getDateFileIngestToArm()).isNull();
        assertThat(dbOsr5.getObjectStatus()).isNull();
        assertThat(dbOsr5.getMd5FileIngestToArm()).isNull();
        assertThat(dbOsr5.getIdResponseFile()).isNull();
        assertThat(dbOsr5.getIdResponseCrFile()).isNull();
        assertThat(dbOsr5.getIdResponseUfFile()).isNull();
        assertThat(dbOsr5.getFlagFileDetsCleanupStatus()).isNull();
        assertThat(dbOsr5.getDateFileDetsCleanup()).isNull();
    }

    @Test
    void batchProcessResponseFiles_WithInvalidTranscriptionChecksum_UpdateObjectStateRecords() throws IOException {

        // given
        authorisationStub.givenTestSchema();
        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();

        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        TranscriptionDocumentEntity transcriptionDocumentEntity = PersistableFactory.getTranscriptionDocument()
            .someMinimalBuilder().transcription(transcriptionEntity).fileName(fileName).fileType(fileType)
            .fileSize(fileSize).uploadedBy(testUser).checksum(checksum).build();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        transcriptionDocumentEntity = dartsPersistence.save(transcriptionDocumentEntity);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().transcriptionDocumentEntity(transcriptionDocumentEntity).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID())
            .checksum("55555555")
            .build();

        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        armEod = dartsPersistence.save(armEod);

        ObjectStateRecordEntity osr = new ObjectStateRecordEntity();
        osr.setUuid(1L);
        osr.setArmEodId(String.valueOf(armEod.getId()));
        osrRepository.save(osr);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DETS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("dropzone/DARTS/response/%s_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp", hashcode1);
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

        OffsetDateTime currentDateTime = transcriptionEntity.getEndTime();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(currentDateTime);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(prefix());
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE);

        // then
        ObjectStateRecordEntity dbOsr = osrRepository.findByArmEodId(String.valueOf(armEod.getId())).orElseThrow();
        assertThat(dbOsr.getFlagRspnRecvdFromArml()).isTrue();
        assertThat(dbOsr.getDateRspnRecvdFromArml()).isCloseTo(currentDateTime, within(1, SECONDS));
        assertThat(dbOsr.getFlagFileIngestStatus()).isFalse();
        assertThat(dbOsr.getDateFileIngestToArm()).isCloseTo(currentDateTime, within(1, SECONDS));
        assertThat(dbOsr.getObjectStatus()).isEqualTo("External object id 1 checksum differs. Arm checksum: 1234 Object Checksum: 55555555");
        assertThat(dbOsr.getMd5FileIngestToArm()).isNull();
        assertThat(dbOsr.getIdResponseFile()).isNull();
        assertThat(dbOsr.getIdResponseCrFile()).isNull();
        assertThat(dbOsr.getIdResponseUfFile()).isNull();
        assertThat(dbOsr.getFlagFileDetsCleanupStatus()).isNull();
        assertThat(dbOsr.getDateFileDetsCleanup()).isNull();
    }

    @Test
    void batchProcessResponseFiles_WithErrorCodeInResponse_UpdateObjectStateRecords() throws IOException {

        // given
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimalBuilder().caseNumber("Case1")
            .courthouse(dartsDatabase.getCourthouseStub().createCourthouseUnlessExists("Bristol")).build();
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        CaseDocumentEntity caseDocument = PersistableFactory.getCaseDocumentTestData()
            .someMinimalBuilder().courtCase(courtCaseEntity).lastModifiedBy(uploadedBy).build();
        caseDocument.setFileName("test_case_document.docx");
        caseDocument = dartsPersistence.save(caseDocument);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().caseDocument(caseDocument).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID()).build();

        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        armEod = dartsPersistence.save(armEod);

        ObjectStateRecordEntity osr1 = new ObjectStateRecordEntity();
        osr1.setUuid(1L);
        osr1.setArmEodId(String.valueOf(armEod.getId()));
        osrRepository.save(osr1);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DETS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename2 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_1_cr.rsp", hashcode1);
        String uploadFileFileFilename2 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_0_uf.rsp", hashcode1);
        List<String> hashcodeResponses = List.of(createRecordFilename2, uploadFileFileFilename2);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String createRecordFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String uploadFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/InvalidResponses/UploadFile.rsp";

        BinaryData createRecordBinaryDataTest2 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest2, armEod.getId()));
        BinaryData uploadFileBinaryDataTest2 = convertStringToBinaryData(getInvalidLineFileContents(uploadFileTest2, armEod.getId()));

        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenReturn(createRecordBinaryDataTest2);
        when(armDataManagementApi.getBlobData(uploadFileFileFilename2)).thenReturn(uploadFileBinaryDataTest2);

        when(armDataManagementApi.deleteBlobData(createRecordFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFileFilename2)).thenReturn(true);

        OffsetDateTime currentDateTime = caseDocument.getCreatedDateTime();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(currentDateTime);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(prefix());
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE);

        // then
        ObjectStateRecordEntity dbOsr = osrRepository.findByArmEodId(String.valueOf(armEod.getId())).orElseThrow();
        assertThat(dbOsr.getFlagRspnRecvdFromArml()).isTrue();
        assertThat(dbOsr.getDateRspnRecvdFromArml()).isCloseTo(currentDateTime, within(1, SECONDS));
        assertThat(dbOsr.getFlagFileIngestStatus()).isFalse();
        assertThat(dbOsr.getDateFileIngestToArm()).isCloseTo(currentDateTime, within(1, SECONDS));
        assertThat(dbOsr.getObjectStatus()).isEqualTo("Exception Description");
        assertThat(dbOsr.getMd5FileIngestToArm()).isNull();
        assertThat(dbOsr.getIdResponseFile()).isNull();
        assertThat(dbOsr.getIdResponseCrFile()).isNull();
        assertThat(dbOsr.getIdResponseUfFile()).isNull();
        assertThat(dbOsr.getFlagFileDetsCleanupStatus()).isNull();
        assertThat(dbOsr.getDateFileDetsCleanup()).isNull();
    }

    @Override
    @Test
    void batchProcessResponseFiles_updateEodWithArmMissingResponse_WhenNoResponseFileGenerated() {
        //given
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimal();

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ObjectStateRecordEntity objectStateRecordEntity = dartsDatabase.getObjectStateRecordRepository()
            .save(createObjectStateRecordEntity(111L));

        ExternalObjectDirectoryEntity armEod1 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID()).build();
        armEod1.setTransferAttempts(1);
        armEod1.setVerificationAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        armEod1.setChecksum("7017013d05bcc5032e142049081821d6");
        armEod1.setOsrUuid(objectStateRecordEntity.getUuid());
        armEod1.setObjectStateRecordEntity(objectStateRecordEntity);
        dartsPersistence.save(armEod1);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(null);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(prefix());
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE);

        // then
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, externalObjectDirectoryEntities.size());
        ExternalObjectDirectoryEntity foundEod = externalObjectDirectoryEntities.getFirst();
        assertEquals("2023-06-10T14:08:28.316382Z", foundEod.getInputUploadProcessedTs().toString());
        assertEquals(ARM_MISSING_RESPONSE.getId(), foundEod.getStatus().getId());
        assertEquals(111L, foundEod.getObjectStateRecordEntity().getUuid());
    }

    @Test
    void batchProcessResponseFiles_updateEodWithArmMissingResponse_WhenNoResponseFileGeneratedAndNoOsrObject() {
        //given
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimal();

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID()).build();
        armEod1.setTransferAttempts(1);
        armEod1.setVerificationAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        armEod1.setChecksum("7017013d05bcc5032e142049081821d6");
        dartsPersistence.save(armEod1);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(null);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getContinuationTokenDuration()).thenReturn("PT1M");
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(prefix());
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE);

        // then
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, externalObjectDirectoryEntities.size());
        ExternalObjectDirectoryEntity foundEod = externalObjectDirectoryEntities.getFirst();
        assertEquals("2023-06-10T14:08:28.316382Z", foundEod.getInputUploadProcessedTs().toString());
        assertEquals(ARM_MISSING_RESPONSE.getId(), foundEod.getStatus().getId());
        assertEquals(null, foundEod.getObjectStateRecordEntity());
    }

    private ObjectStateRecordEntity createObjectStateRecordEntity(Long uuid) {
        ObjectStateRecordEntity objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(uuid);
        return objectStateRecordEntity;
    }

}