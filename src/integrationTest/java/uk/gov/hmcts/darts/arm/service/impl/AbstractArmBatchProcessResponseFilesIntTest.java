package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.exception.AzureException;
import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.arm.service.DeleteArmResponseFilesHelper;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
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
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AsyncTaskConfig;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.util.AsyncUtil;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MISSING_RESPONSE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@Slf4j
@SuppressWarnings({"VariableDeclarationUsageDistance", "PMD.NcssCount", "PMD.ExcessiveImports"})
abstract class AbstractArmBatchProcessResponseFilesIntTest extends IntegrationBase {

    private static final LocalDateTime HEARING_DATETIME = LocalDateTime.of(2023, 6, 10, 10, 0, 0);
    private static final String DATETIMEKEY = "<datetimekey>";
    private static final String INPUT_UPLOAD_RESPONSE_DATETIME = "2023-06-10T14:08:28.316382+00:00";
    private static final String HASHCODE_2 = "7a374f19a9ce7dc9cc480ea8d4eca0fc";
    private static final String INPUT_UPLOAD_RESPONSE = """
            {
                "operation": "input_upload",
                "timestamp": "<datetimekey>",
                "status": 1,
                "exception_description": null,
                "error_status": null,
                "filename": "DARTS_fa292f18-55e7-4d58-b610-0435a37900a2",
                "submission_folder": "/dropzone/DARTS/submission",
                "file_hash": "a11f992a43ea6d0b192d57fe44403942"
            }
        """;

    protected static final String T_13_00_00_Z = "2023-06-10T13:00:00Z";
    protected static final String T_13_45_00_Z = "2023-06-10T13:45:00Z";

    @Autowired
    protected ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Autowired
    protected ExternalObjectDirectoryService externalObjectDirectoryService;
    @MockitoBean
    protected ArmDataManagementApi armDataManagementApi;
    @Autowired
    protected FileOperationService fileOperationService;
    @Autowired
    protected ArmDataManagementConfiguration armDataManagementConfiguration;
    @Autowired
    protected ObjectMapper objectMapper;
    @MockitoBean
    protected UserIdentity userIdentity;
    @Mock
    protected CurrentTimeHelper currentTimeHelper;
    @Autowired
    protected DeleteArmResponseFilesHelper deleteArmResponseFilesHelper;

    @Autowired
    protected AuthorisationStub authorisationStub;
    @Autowired
    protected LogApi logApi;

    protected AbstractArmBatchProcessResponseFiles armBatchProcessResponseFiles;
    protected String continuationToken;

    protected static final Integer BATCH_SIZE = 10;
    protected OffsetDateTime inputUploadProcessedTimestamp;
    protected AsyncTaskConfig asyncTaskConfig;

    @BeforeEach
    void commonSetup() {
        asyncTaskConfig = mock(AsyncTaskConfig.class);
        when(asyncTaskConfig.getThreads()).thenReturn(1);
        when(asyncTaskConfig.getAsyncTimeout()).thenReturn(Duration.ofSeconds(10));
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        String dateTimeFormatStr = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS[XXXX][XXXXX]";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormatStr);
        inputUploadProcessedTimestamp = OffsetDateTime.parse(INPUT_UPLOAD_RESPONSE_DATETIME, formatter);

        String inputUploadResponse = INPUT_UPLOAD_RESPONSE.replace(DATETIMEKEY, INPUT_UPLOAD_RESPONSE_DATETIME);
        BinaryData inputUploadFileRecord = convertStringToBinaryData(inputUploadResponse);

        when(armDataManagementApi.getBlobData(Mockito.startsWith("dropzone/DARTS/response/" + prefix() + "_"))).thenReturn(inputUploadFileRecord);
    }

    protected abstract String prefix();

    @Test
    void batchProcessResponseFiles_WithMediaReturnsSuccess() throws IOException {

        // given
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimal();

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

        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod1.setTransferAttempts(1);
        armEod1.setVerificationAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        armEod1.setChecksum("7017013d05bcc5032e142049081821d6");
        armEod1 = dartsPersistence.save(armEod1);

        ExternalObjectDirectoryEntity armEod2 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media2).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod2.setTransferAttempts(1);
        armEod2.setManifestFile(manifestFile1);
        OffsetDateTime armEod2InputUploadProcessedTs = OffsetDateTime.parse("2027-06-10T13:30:00Z");
        armEod2.setInputUploadProcessedTs(armEod2InputUploadProcessedTs);
        armEod2.setChecksum("7017013d05bcc5032e142049081821d6");
        armEod2.setVerificationAttempts(1);
        armEod2 = dartsPersistence.save(armEod2);

        ExternalObjectDirectoryEntity armEod3 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media3).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod3.setTransferAttempts(1);
        armEod3.setManifestFile(manifestFile1);
        armEod3.setVerificationAttempts(1);
        armEod3 = dartsPersistence.save(armEod3);

        ExternalObjectDirectoryEntity armEod4 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media4).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod4.setTransferAttempts(1);
        armEod4.setManifestFile(manifestFile1);
        armEod4.setVerificationAttempts(1);
        armEod4 = dartsPersistence.save(armEod4);

        String manifestFile2 = prefix() + "_" + manifest2Uuid + ".a360";
        ExternalObjectDirectoryEntity armEod5 = PersistableFactory
            .getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media5).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod5.setTransferAttempts(1);
        armEod5.setManifestFile(manifestFile2);
        armEod5.setVerificationAttempts(1);
        armEod5 = dartsPersistence.save(armEod5);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        String blobNameAndPath2 = String.format("dropzone/DARTS/response/%s_%s_7a374f19a9ce7dc9cc480ea8d4eca0fc_1_iu.rsp", prefix(), manifest2Uuid);
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
        String invalidLineFileFilename4 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1892_0_il.rsp", hashcode1);
        String invalidLineFileFilename5 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1893_0_il.rsp", hashcode1);

        List<String> hashcodeResponses = List.of(createRecordFilename1, uploadFileFilename1,
                                                 createRecordFilename2, invalidLineFileFilename2,
                                                 uploadFileFilename3, invalidLineFileFilename3,
                                                 invalidLineFileFilename4, invalidLineFileFilename5);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String validUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
        String createRecordFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String invalidLineFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";
        String validUploadFileTest3 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
        String invalidLineFileTest3 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";
        String invalidLineFileTest4 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";
        String invalidLineFileTest5 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile2.rsp";

        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, armEod1.getId()));
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(getUploadFileContents(validUploadFileTest1, armEod1.getId(), media1.getChecksum()));
        BinaryData createRecordBinaryDataTest2 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest2, armEod2.getId()));
        BinaryData invalidLineFileBinaryDataTest2 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest2, armEod2.getId()));
        BinaryData uploadFileBinaryDataTest3 = convertStringToBinaryData(getUploadFileContents(validUploadFileTest3, armEod3.getId(), media3.getChecksum()));
        BinaryData invalidLineFileBinaryDataTest3 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest3, armEod3.getId()));
        BinaryData invalidLineFileBinaryDataTest4 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest4, armEod4.getId()));
        BinaryData invalidLineFileBinaryDataTest5 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest5, armEod4.getId()));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);
        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenReturn(createRecordBinaryDataTest2);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename2)).thenReturn(invalidLineFileBinaryDataTest2);
        when(armDataManagementApi.getBlobData(uploadFileFilename3)).thenReturn(uploadFileBinaryDataTest3);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename3)).thenReturn(invalidLineFileBinaryDataTest3);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename4)).thenReturn(invalidLineFileBinaryDataTest4);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename5)).thenReturn(invalidLineFileBinaryDataTest5);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename3)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename3)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename4)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename5)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(endTime2);

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);
        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenReturn(createRecordBinaryDataTest2);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename2)).thenReturn(invalidLineFileBinaryDataTest2);
        when(armDataManagementApi.getBlobData(uploadFileFilename3)).thenReturn(uploadFileBinaryDataTest3);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename3)).thenReturn(invalidLineFileBinaryDataTest3);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename4)).thenReturn(invalidLineFileBinaryDataTest4);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename5)).thenReturn(invalidLineFileBinaryDataTest5);

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
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename4)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename5)).thenReturn(true);

        when(armDataManagementApi.deleteBlobData(createRecordFilename5)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(endTime2);

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();

        assertEquals(inputUploadProcessedTimestamp, foundMedia.getInputUploadProcessedTs());

        assertEquals(ARM_RPO_PENDING.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertNotNull(foundMedia.getDataIngestionTs());
        assertTrue(foundMedia.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList2 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media2, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList2.size());
        ExternalObjectDirectoryEntity foundMedia2 = foundMediaList2.getFirst();
        //Make sure the inputUploadProcessedTs is not updated if already set
        assertEquals(armEod2InputUploadProcessedTs, foundMedia2.getInputUploadProcessedTs());
        assertEquals(ARM_RESPONSE_MANIFEST_FAILED.getId(), foundMedia2.getStatus().getId());
        assertEquals(2, foundMedia2.getVerificationAttempts());
        assertEquals(1, foundMedia2.getTransferAttempts());
        assertTrue(foundMedia2.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList3 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media3, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList3.size());
        ExternalObjectDirectoryEntity foundMedia3 = foundMediaList3.getFirst();
        assertEquals("2023-06-10T14:08:28.316382Z", foundMedia3.getInputUploadProcessedTs().toString());
        assertEquals(ARM_RESPONSE_MANIFEST_FAILED.getId(), foundMedia3.getStatus().getId());
        assertEquals(2, foundMedia3.getVerificationAttempts());
        assertEquals(1, foundMedia3.getTransferAttempts());
        assertTrue(foundMedia3.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList4 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media4, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList4.size());
        ExternalObjectDirectoryEntity foundMedia4 = foundMediaList4.getFirst();
        assertEquals("2023-06-10T14:08:28.316382Z", foundMedia4.getInputUploadProcessedTs().toString());
        assertEquals(ARM_RESPONSE_MANIFEST_FAILED.getId(), foundMedia4.getStatus().getId());
        assertEquals(2, foundMedia4.getVerificationAttempts());
        assertEquals(1, foundMedia4.getTransferAttempts());
        assertTrue(foundMedia4.isResponseCleaned());
        assertEquals(
            "Operation: create_record - PS.20023:INVALID_PARAMETERS:Invalid line: invalid json; "
                + "Operation: upload_new_file - PS.20042:INVALID_RELATION_ID:No create_record operation with specified relation_id in the same input file.; ",
            foundMedia4.getErrorCode());

        List<ExternalObjectDirectoryEntity> foundMediaList5 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media5, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList5.size());
        ExternalObjectDirectoryEntity foundMedia5 = foundMediaList5.getFirst();
        assertEquals("2023-06-10T14:08:28.316382Z", foundMedia5.getInputUploadProcessedTs().toString());
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia5.getStatus().getId());
        assertEquals(1, foundMedia5.getVerificationAttempts());
        assertEquals(1, foundMedia5.getTransferAttempts());
        assertFalse(foundMedia5.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken);
        verify(armDataManagementApi).listResponseBlobs(hashcode1);

        verify(armDataManagementApi).getBlobData(createRecordFilename1);
        verify(armDataManagementApi).getBlobData(createRecordFilename2);

        verify(armDataManagementApi).getBlobData(uploadFileFilename1);
        verify(armDataManagementApi).getBlobData(uploadFileFilename3);

        verify(armDataManagementApi).getBlobData(invalidLineFileFilename2);
        verify(armDataManagementApi).getBlobData(invalidLineFileFilename3);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename2);
        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename2);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename1);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename1);

        verify(armDataManagementApi).deleteBlobData(uploadFileFilename3);
        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename3);

        verify(armDataManagementApi).deleteBlobData(blobNameAndPath1);

        verify(armDataManagementApi).listResponseBlobs(hashcode2);
        verify(armDataManagementApi).getBlobData(createRecordFilename5);

        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename4);
        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename5);

        verify(armDataManagementApi, never()).deleteBlobData(blobNameAndPath2);
    }

    @Test
    void batchProcessResponseFiles_WithInvalidLineFileAndCreateRecordFileSuccess() throws IOException {

        // given
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimal();

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(endTime);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod1.setTransferAttempts(1);
        armEod1.setVerificationAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        armEod1.setChecksum("7017013d05bcc5032e142049081821d6");
        armEod1 = dartsPersistence.save(armEod1);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String invalidLineFileFilename2 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1893_0_il.rsp", hashcode1);

        List<String> hashcodeResponses = List.of(createRecordFilename1, invalidLineFileFilename2);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String invalidLineFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";

        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, armEod1.getId()));
        BinaryData invalidLineFileBinaryDataTest2 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest2, armEod1.getId()));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename2)).thenReturn(invalidLineFileBinaryDataTest2);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename2)).thenReturn(true);

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, externalObjectDirectoryEntities.size());
        ExternalObjectDirectoryEntity foundEod = externalObjectDirectoryEntities.getFirst();
        assertEquals(inputUploadProcessedTimestamp, foundEod.getInputUploadProcessedTs());
        assertEquals(ARM_RESPONSE_MANIFEST_FAILED.getId(), foundEod.getStatus().getId());
        assertEquals(2, foundEod.getVerificationAttempts());
        assertEquals("Operation: create_record - PS.20023:INVALID_PARAMETERS:Invalid line: invalid json; ", foundEod.getErrorCode());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken);
        verify(armDataManagementApi).listResponseBlobs(hashcode1);

        verify(armDataManagementApi).getBlobData(createRecordFilename1);
        verify(armDataManagementApi).getBlobData(invalidLineFileFilename2);
        verify(armDataManagementApi).getBlobData(blobNameAndPath1);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename1);
        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename2);
        verify(armDataManagementApi).deleteBlobData(blobNameAndPath1);

        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void runTasksAsync_asyncException() {

        try (MockedStatic<AsyncUtil> asyncUtilMockedStatic = Mockito.mockStatic(AsyncUtil.class)) {
            asyncUtilMockedStatic.when(() -> AsyncUtil.invokeAllAwaitTermination(any(), any()))
                .thenThrow(new RuntimeException("Test exception"));
            armBatchProcessResponseFiles.runTasksAsync(new ArrayList<>(), asyncTaskConfig);
            //Should be gracefully handled
        }
    }

    @Test
    void batchProcessResponseFiles_successful_withUploadFileAndCreateRecordFile() throws IOException {

        // given
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimal();

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(endTime);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod1.setTransferAttempts(1);
        armEod1.setVerificationAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        armEod1.setChecksum("7017013d05bcc5032e142049081821d6");
        armEod1 = dartsPersistence.save(armEod1);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp", hashcode1);

        List<String> hashcodeResponses = List.of(createRecordFilename1, uploadFileFilename1);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String inputUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InputUploadFile.rsp";
        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String validUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";

        BinaryData inputUploadFileBinaryDataTest1 = convertStringToBinaryData(getInputUploadFileContents(inputUploadFileTest1));
        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, armEod1.getId()));
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(getUploadFileContents(validUploadFileTest1, armEod1.getId(), media1.getChecksum()));

        when(armDataManagementApi.getBlobData(blobNameAndPath1)).thenReturn(inputUploadFileBinaryDataTest1);
        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(ARM_RPO_PENDING.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertNotNull(foundMedia.getDataIngestionTs());
        assertTrue(foundMedia.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken);
        verify(armDataManagementApi).listResponseBlobs(hashcode1);

        verify(armDataManagementApi).getBlobData(createRecordFilename1);
        verify(armDataManagementApi).getBlobData(uploadFileFilename1);
        verify(armDataManagementApi).getBlobData(blobNameAndPath1);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename1);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename1);
        verify(armDataManagementApi).deleteBlobData(blobNameAndPath1);

        verifyNoMoreInteractions(armDataManagementApi);

    }

    @Test
    void batchProcessResponseFiles_throwsException_whenUploadFileIsInvalid() throws IOException {

        // given
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimal();

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(endTime);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod1.setTransferAttempts(1);
        armEod1.setVerificationAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        armEod1.setChecksum("7017013d05bcc5032e142049081821d6");
        armEod1 = dartsPersistence.save(armEod1);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp", hashcode1);

        List<String> hashcodeResponses = List.of(createRecordFilename1, uploadFileFilename1);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String inputUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/InvalidResponses/InputUploadFile.rsp";
        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String validUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";

        BinaryData inputUploadFileBinaryDataTest1 = convertStringToBinaryData(getInputUploadFileContents(inputUploadFileTest1));
        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, armEod1.getId()));
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(getUploadFileContents(validUploadFileTest1, armEod1.getId(), media1.getChecksum()));

        when(armDataManagementApi.getBlobData(blobNameAndPath1)).thenReturn(inputUploadFileBinaryDataTest1);
        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(ARM_RESPONSE_PROCESSING_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertNull(foundMedia.getDataIngestionTs());
        assertFalse(foundMedia.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken);
        verify(armDataManagementApi).listResponseBlobs(hashcode1);

        verify(armDataManagementApi).getBlobData(blobNameAndPath1);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename1);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename1);
        verify(armDataManagementApi).deleteBlobData(blobNameAndPath1);

        verifyNoMoreInteractions(armDataManagementApi);

    }

    @Test
    void batchProcessResponseFiles_With3InvalidLineFilesCausingFailure() throws IOException {

        // given
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimal();

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(endTime);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod1.setTransferAttempts(1);
        armEod1.setVerificationAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        armEod1.setChecksum("7017013d05bcc5032e142049081821d6");
        armEod1 = dartsPersistence.save(armEod1);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String invalidLineFileFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1891_0_il.rsp", hashcode1);
        String invalidLineFileFilename2 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1893_0_il.rsp", hashcode1);
        String invalidLineFileFilename3 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_0_il.rsp", hashcode1);

        List<String> hashcodeResponses = List.of(invalidLineFileFilename1, invalidLineFileFilename2, invalidLineFileFilename3);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String invalidLineFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";
        String invalidLineFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";
        String invalidLineFileTest3 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";

        BinaryData invalidLineFileBinaryDataTest1 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest1, armEod1.getId()));
        BinaryData invalidLineFileBinaryDataTest2 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest2, armEod1.getId()));
        BinaryData invalidLineFileBinaryDataTest3 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest3, armEod1.getId()));

        when(armDataManagementApi.getBlobData(invalidLineFileFilename1)).thenReturn(invalidLineFileBinaryDataTest1);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename2)).thenReturn(invalidLineFileBinaryDataTest2);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename3)).thenReturn(invalidLineFileBinaryDataTest3);

        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename3)).thenReturn(true);

        when(armDataManagementApi.getBlobData(invalidLineFileFilename1)).thenReturn(invalidLineFileBinaryDataTest1);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename2)).thenReturn(invalidLineFileBinaryDataTest2);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename3)).thenReturn(invalidLineFileBinaryDataTest3);

        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename3)).thenReturn(true);

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByManifestFile(manifestFile1);

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(inputUploadProcessedTimestamp, foundMedia.getInputUploadProcessedTs());
        assertEquals(ARM_RESPONSE_PROCESSING_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken);
        verify(armDataManagementApi).listResponseBlobs(hashcode1);

        verify(armDataManagementApi).getBlobData(invalidLineFileFilename1);
        verify(armDataManagementApi).getBlobData(invalidLineFileFilename2);
        verify(armDataManagementApi).getBlobData(invalidLineFileFilename3);

        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename1);
        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename2);
        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename3);

        verify(armDataManagementApi).getBlobData(blobNameAndPath1);

        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void batchProcessResponseFiles_WithMediaUsingSmallContinuationTokenReturnsSuccess() throws IOException {
        // We want to update the existing config value, but we need to set it back to the original afterward so that the change doesn't affect other tests.
        // Yes, this is ugly, there must be a better way.
        final Integer originalMaxContinuationBatchSize = armDataManagementConfiguration.getMaxContinuationBatchSize();
        armDataManagementConfiguration.setMaxContinuationBatchSize(1);

        // given
        HearingEntity hearing = dartsDatabase.createHearing("NEWCASTLE", "Int Test Courtroom 2", "2", HEARING_DATETIME);

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

        ExternalObjectDirectoryEntity armEod1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1, ARM_DROP_ZONE, ARM, UUID.randomUUID().toString());
        armEod1.setTransferAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        armEod1.setChecksum("7017013d05bcc5032e142049081821d6");
        dartsDatabase.save(armEod1);

        ExternalObjectDirectoryEntity armEod2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media2, ARM_DROP_ZONE, ARM, UUID.randomUUID().toString());
        armEod2.setTransferAttempts(1);
        armEod2.setManifestFile(manifestFile1);
        armEod2.setChecksum("7017013d05bcc5032e142049081821d6");
        dartsDatabase.save(armEod2);

        ExternalObjectDirectoryEntity armEod3 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media3, ARM_DROP_ZONE, ARM, UUID.randomUUID().toString());
        armEod3.setTransferAttempts(1);
        armEod3.setManifestFile(manifestFile1);
        dartsDatabase.save(armEod3);

        String manifestFile2 = prefix() + "_" + manifest2Uuid + ".a360";
        ExternalObjectDirectoryEntity armEod5 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media5, ARM_DROP_ZONE, ARM, UUID.randomUUID().toString());
        armEod5.setTransferAttempts(1);
        armEod5.setManifestFile(manifestFile2);
        dartsDatabase.save(armEod5);

        List<String> blobNamesAndPaths1 = new ArrayList<>();
        List<String> blobNamesAndPaths2 = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        String blobNameAndPath2 = String.format("dropzone/DARTS/response/%s_%s_7a374f19a9ce7dc9cc480ea8d4eca0fc_1_iu.rsp", prefix(), manifest2Uuid);
        blobNamesAndPaths1.add(blobNameAndPath1);
        blobNamesAndPaths2.add(blobNameAndPath2);

        ContinuationTokenBlobs continuationTokenBlobs1 = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths1)
            .continuationToken("13259aae1895")
            .build();
        ContinuationTokenBlobs continuationTokenBlobs2 = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths2)
            .build();
        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), 1, null)).thenReturn(continuationTokenBlobs1);
        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), 1, "13259aae1895")).thenReturn(continuationTokenBlobs2);
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

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(inputUploadProcessedTimestamp, foundMedia.getInputUploadProcessedTs());
        assertEquals(ARM_RPO_PENDING.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertNotNull(foundMedia.getDataIngestionTs());
        assertTrue(foundMedia.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList2 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media2, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList2.size());
        ExternalObjectDirectoryEntity foundMedia2 = foundMediaList2.getFirst();
        assertEquals(inputUploadProcessedTimestamp, foundMedia2.getInputUploadProcessedTs());
        assertEquals(ARM_RESPONSE_MANIFEST_FAILED.getId(), foundMedia2.getStatus().getId());
        assertEquals(2, foundMedia2.getVerificationAttempts());
        assertEquals(1, foundMedia2.getTransferAttempts());
        assertTrue(foundMedia2.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList3 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media3, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList3.size());
        ExternalObjectDirectoryEntity foundMedia3 = foundMediaList3.getFirst();
        assertEquals(inputUploadProcessedTimestamp, foundMedia3.getInputUploadProcessedTs());
        assertEquals(ARM_RESPONSE_MANIFEST_FAILED.getId(), foundMedia3.getStatus().getId());
        assertEquals(2, foundMedia3.getVerificationAttempts());
        assertEquals(1, foundMedia3.getTransferAttempts());
        assertTrue(foundMedia3.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList5 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media5, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList5.size());
        ExternalObjectDirectoryEntity foundMedia5 = foundMediaList5.getFirst();
        assertEquals(inputUploadProcessedTimestamp, foundMedia5.getInputUploadProcessedTs());
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia5.getStatus().getId());
        assertEquals(1, foundMedia5.getVerificationAttempts());
        assertFalse(foundMedia5.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(prefix(), 1, continuationToken);
        verify(armDataManagementApi).listResponseBlobs(hashcode1);

        verify(armDataManagementApi).getBlobData(createRecordFilename1);
        verify(armDataManagementApi).getBlobData(createRecordFilename2);

        verify(armDataManagementApi).getBlobData(uploadFileFilename1);
        verify(armDataManagementApi).getBlobData(uploadFileFilename3);

        verify(armDataManagementApi).getBlobData(invalidLineFileFilename2);
        verify(armDataManagementApi).getBlobData(invalidLineFileFilename3);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename2);
        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename2);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename1);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename1);

        verify(armDataManagementApi).deleteBlobData(uploadFileFilename3);
        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename3);

        verify(armDataManagementApi).deleteBlobData(blobNameAndPath1);

        verify(armDataManagementApi).listResponseBlobs(hashcode2);
        verify(armDataManagementApi).getBlobData(createRecordFilename5);

        armDataManagementConfiguration.setMaxContinuationBatchSize(originalMaxContinuationBatchSize);
    }

    @Test
    void batchProcessResponseFiles_GetBlobsThrowsException() throws IOException {

        // given
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimal();

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

        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 =
            PersistableFactory.getExternalObjectDirectoryTestData()
                .someMinimalBuilder().media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
                .verificationAttempts(1).transferAttempts(1)
                .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();

        armEod1.setManifestFile(manifestFile1);
        dartsPersistence.save(armEod1);

        ExternalObjectDirectoryEntity armEod2 = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().media(media2).status(dartsDatabase
                                                           .getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();

        armEod2.setTransferAttempts(1);
        armEod2.setManifestFile(manifestFile1);
        dartsPersistence.save(armEod2);

        ExternalObjectDirectoryEntity armEod3 = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().media(media3).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();

        armEod3.setTransferAttempts(1);
        armEod3.setManifestFile(manifestFile1);
        dartsPersistence.save(armEod3);

        ExternalObjectDirectoryEntity armEod4 = PersistableFactory
            .getExternalObjectDirectoryTestData()
            .someMinimalBuilder().media(media4).status(dartsDatabase
                                                           .getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();

        armEod4.setTransferAttempts(1);
        armEod4.setManifestFile(manifestFile1);
        dartsPersistence.save(armEod4);

        String manifestFile2 = prefix() + "_" + manifest2Uuid + ".a360";
        ExternalObjectDirectoryEntity armEod5 = PersistableFactory
            .getExternalObjectDirectoryTestData().someMinimalBuilder().media(media5).status(dartsDatabase
                                                                                                .getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod5.setTransferAttempts(1);
        armEod5.setManifestFile(manifestFile2);
        dartsPersistence.save(armEod5);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        String blobNameAndPath2 = String.format("dropzone/DARTS/response/%s_%s_7a374f19a9ce7dc9cc480ea8d4eca0fc_1_iu.rsp", prefix(), manifest2Uuid);
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

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenThrow(new RuntimeException());
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenThrow(new RuntimeException());
        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenThrow(new RuntimeException());
        when(armDataManagementApi.getBlobData(invalidLineFileFilename2)).thenThrow(new RuntimeException());
        when(armDataManagementApi.getBlobData(uploadFileFilename3)).thenThrow(new RuntimeException());
        when(armDataManagementApi.getBlobData(invalidLineFileFilename3)).thenThrow(new RuntimeException());

        String createRecordFilename4 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1890_1_cr.rsp", HASHCODE_2);
        List<String> hashcodeResponses2 = List.of(createRecordFilename4);
        when(armDataManagementApi.listResponseBlobs(HASHCODE_2)).thenReturn(hashcodeResponses2);
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

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(inputUploadProcessedTimestamp, foundMedia.getInputUploadProcessedTs());
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList2 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media2, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList2.size());
        ExternalObjectDirectoryEntity foundMedia2 = foundMediaList2.getFirst();
        assertEquals(inputUploadProcessedTimestamp, foundMedia2.getInputUploadProcessedTs());
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia2.getStatus().getId());
        assertEquals(1, foundMedia2.getVerificationAttempts());
        assertFalse(foundMedia2.isResponseCleaned());


        List<ExternalObjectDirectoryEntity> foundMediaList3 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media3, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList3.size());
        ExternalObjectDirectoryEntity foundMedia3 = foundMediaList3.getFirst();
        assertEquals(inputUploadProcessedTimestamp, foundMedia3.getInputUploadProcessedTs());
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia3.getStatus().getId());
        assertEquals(1, foundMedia3.getVerificationAttempts());
        assertFalse(foundMedia3.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList4 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media4, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList4.size());
        ExternalObjectDirectoryEntity foundMedia4 = foundMediaList4.getFirst();
        assertEquals(inputUploadProcessedTimestamp, foundMedia4.getInputUploadProcessedTs());
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia4.getStatus().getId());
        assertEquals(1, foundMedia4.getVerificationAttempts());
        assertFalse(foundMedia4.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList5 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media5, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList5.size());
        ExternalObjectDirectoryEntity foundMedia5 = foundMediaList5.getFirst();
        assertEquals(inputUploadProcessedTimestamp, foundMedia5.getInputUploadProcessedTs());
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia5.getStatus().getId());
        assertEquals(1, foundMedia5.getVerificationAttempts());
        assertFalse(foundMedia5.isResponseCleaned());
    }

    @Test
    void batchProcessResponseFiles_WithInvalidJson() throws IOException {

        // given
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimal();

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

        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod1.setTransferAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        armEod1 = dartsPersistence.save(armEod1);

        ExternalObjectDirectoryEntity armEod2 = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().media(media2).status(dartsDatabase
                                                           .getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod2.setTransferAttempts(1);
        armEod2.setManifestFile(manifestFile1);
        armEod2 = dartsPersistence.save(armEod2);

        ExternalObjectDirectoryEntity armEod3 = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().media(media3).status(dartsDatabase
                                                           .getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod3.setTransferAttempts(1);
        armEod3.setManifestFile(manifestFile1);
        armEod3 = dartsPersistence.save(armEod3);

        ExternalObjectDirectoryEntity armEod4 = PersistableFactory
            .getExternalObjectDirectoryTestData().someMinimalBuilder().media(media4).status(dartsDatabase
                                                                                                .getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod4.setTransferAttempts(1);
        armEod4.setManifestFile(manifestFile1);
        armEod4 = dartsPersistence.save(armEod4);

        String manifestFile2 = prefix() + "_" + manifest2Uuid + ".a360";
        ExternalObjectDirectoryEntity armEod5 = PersistableFactory
            .getExternalObjectDirectoryTestData().someMinimalBuilder().media(media5).status(dartsDatabase
                                                                                                .getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod5.setTransferAttempts(1);
        armEod5.setManifestFile(manifestFile2);
        dartsPersistence.save(armEod5);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        String blobNameAndPath2 = String.format("dropzone/DARTS/response/%s_%s_7a374f19a9ce7dc9cc480ea8d4eca0fc_1_iu.rsp", prefix(), manifest2Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);
        blobNamesAndPaths.add(blobNameAndPath2);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("dropzone/DARTS/response/%s_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp", hashcode1);
        String createRecordFilename2 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_1_cr.rsp", hashcode1);
        String invalidLineFileFilename2 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_0_il.rsp", hashcode1);
        String uploadFileFilename3 = String.format("dropzone/DARTS/response/%s_04e6bc3b-952a-79b6-8362-13259aae1897_1_uf.rsp", hashcode1);
        String invalidLineFileFilename3 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1897_0_il.rsp", hashcode1);
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
        String createRecordFilename4 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1890_1_cr.rsp", hashcode2);
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

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList2 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media2, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList2.size());
        ExternalObjectDirectoryEntity foundMedia2 = foundMediaList2.getFirst();
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia2.getStatus().getId());
        assertEquals(1, foundMedia2.getVerificationAttempts());
        assertFalse(foundMedia2.isResponseCleaned());


        List<ExternalObjectDirectoryEntity> foundMediaList3 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media3, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList3.size());
        ExternalObjectDirectoryEntity foundMedia3 = foundMediaList3.getFirst();
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia3.getStatus().getId());
        assertEquals(1, foundMedia3.getVerificationAttempts());
        assertFalse(foundMedia3.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList4 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media4, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList4.size());
        ExternalObjectDirectoryEntity foundMedia4 = foundMediaList4.getFirst();
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia4.getStatus().getId());
        assertEquals(1, foundMedia4.getVerificationAttempts());
        assertFalse(foundMedia4.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList5 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media5, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList5.size());
        ExternalObjectDirectoryEntity foundMedia5 = foundMediaList5.getFirst();
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia5.getStatus().getId());
        assertEquals(1, foundMedia5.getVerificationAttempts());
        assertFalse(foundMedia5.isResponseCleaned());
    }

    @Test
    void batchProcessResponseFiles_WithInvalidFilenameStatus() throws IOException {

        // given
        HearingEntity hearing = dartsDatabase.createHearing("NEWCASTLE", "Int Test Courtroom 2", "2", HEARING_DATETIME);

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);

        MediaEntity media2 = createMediaEntity(hearing, startTime, endTime, 2);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod1.setTransferAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        armEod1 = dartsPersistence.save(armEod1);

        ExternalObjectDirectoryEntity armEod2 = PersistableFactory
            .getExternalObjectDirectoryTestData().someMinimalBuilder().media(media2).status(dartsDatabase
                                                                                                .getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod2.setTransferAttempts(1);
        armEod2.setManifestFile(manifestFile1);
        armEod2 = dartsPersistence.save(armEod2);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("dropzone/DARTS/response/%s_04e6bc3b-952a-79b6-8362-13259aae1895_0_uf.rsp", hashcode1);
        String createRecordFilename2 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_0_cr.rsp", hashcode1);
        String invalidLineFileFilename2 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_1_il.rsp", hashcode1);

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

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(ARM_RESPONSE_PROCESSING_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertTrue(foundMedia.isResponseCleaned());

        List<ExternalObjectDirectoryEntity> foundMediaList2 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media2, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList2.size());
        ExternalObjectDirectoryEntity foundMedia2 = foundMediaList2.getFirst();
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
            .externalLocation(UUID.randomUUID().toString()).build();

        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        armEod.setChecksum(checksum);
        armEod = dartsPersistence.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
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
            getUploadFileContents(validUploadFileTest1, armEod.getId(), transcriptionDocumentEntity.getChecksum()));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(transcriptionEntity.getEndTime());

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        ExternalObjectDirectoryEntity foundTranscriptionEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(ARM_RPO_PENDING.getId(), foundTranscriptionEod.getStatus().getId());
        assertEquals("e7cde7c6-15d7-4c7e-a85d-a468c7ea72b9", foundTranscriptionEod.getExternalFileId());
        assertEquals("1cf976c7-cedd-703f-ab70-01588bd56d50", foundTranscriptionEod.getExternalRecordId());
        assertTrue(foundTranscriptionEod.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken);
        verify(armDataManagementApi).listResponseBlobs(hashcode1);

        verify(armDataManagementApi).getBlobData(createRecordFilename1);
        verify(armDataManagementApi).getBlobData(uploadFileFilename1);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename1);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename1);

        verify(armDataManagementApi).deleteBlobData(blobNameAndPath1);
    }

    @NotNull
    protected static List<String> getHashcodeResponses(String hashcode1, String createRecordFilename1, String uploadFileFilename1) {
        String createRecordFilename2 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_1_cr.rsp", hashcode1);
        String invalidLineFileFilename2 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_0_il.rsp", hashcode1);
        String uploadFileFilename3 = String.format("dropzone/DARTS/response/%s_04e6bc3b-952a-79b6-8362-13259aae1897_1_uf.rsp", hashcode1);
        String invalidLineFileFilename3 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1897_0_il.rsp", hashcode1);
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
            .externalLocation(UUID.randomUUID().toString()).build();

        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        armEod = dartsPersistence.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
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

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(transcriptionEntity.getEndTime());

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

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
        AnnotationDocumentEntity annotationDocument = PersistableFactory.getAnnotationDocumentTestData()
            .someMinimalBuilder().annotation(annotation).fileName(fileName).fileType(fileType)
            .fileSize(fileSize).uploadedBy(testUser).uploadedDateTime(uploadedDateTime).checksum(checksum).build();

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().annotationDocumentEntity(annotationDocument).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID().toString()).build();
        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        armEod.setChecksum(checksum);
        armEod = dartsPersistence.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
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
            getUploadFileContents(validUploadFileTest1, armEod.getId(), annotationDocument.getChecksum()));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(annotationDocument.getUploadedDateTime());

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        ExternalObjectDirectoryEntity foundAnnotationEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(ARM_RPO_PENDING.getId(), foundAnnotationEod.getStatus().getId());
        assertEquals("e7cde7c6-15d7-4c7e-a85d-a468c7ea72b9", foundAnnotationEod.getExternalFileId());
        assertEquals("1cf976c7-cedd-703f-ab70-01588bd56d50", foundAnnotationEod.getExternalRecordId());
        assertTrue(foundAnnotationEod.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken);
        verify(armDataManagementApi).listResponseBlobs(hashcode1);

        verify(armDataManagementApi).getBlobData(createRecordFilename1);
        verify(armDataManagementApi).getBlobData(uploadFileFilename1);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename1);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename1);

        verify(armDataManagementApi).deleteBlobData(blobNameAndPath1);
    }

    @Test
    void batchProcessResponseFiles_WithCaseDocumentReturnsSuccess() throws IOException {

        // given
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimalBuilder().caseNumber("Case1")
            .courthouse(dartsDatabase.getCourthouseStub().createCourthouseUnlessExists("Bristol")).build();
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        courtCaseEntity = dartsPersistence.save(courtCaseEntity);


        CaseDocumentEntity caseDocument = PersistableFactory.getCaseDocumentTestData()
            .someMinimalBuilder().courtCase(courtCaseEntity).checksum("C3CCA7021CF79B42F245AF350601C284\")")
            .lastModifiedById(uploadedBy.getId())
            .build();
        caseDocument.setCourtCase(courtCaseEntity);
        caseDocument.setFileName("test_filename");
        caseDocument.setFileType("docx");
        caseDocument.setFileSize(1234);
        caseDocument.setChecksum("xC3CCA7021CF79B42F245AF350601C284");
        caseDocument.setHidden(false);
        caseDocument.setCreatedBy(uploadedBy);
        caseDocument.setCreatedDateTime(OffsetDateTime.now(UTC));
        caseDocument.setLastModifiedBy(uploadedBy);
        caseDocument.setFileName("test_case_document.docx");

        caseDocument = dartsPersistence.save(caseDocument);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().caseDocument(caseDocument).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID().toString()).checksum("xC3CCA7021CF79B42F245AF350601C284").build();

        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        armEod.setChecksum("xC3CCA7021CF79B42F245AF350601C284");
        armEod = dartsPersistence.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
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
            getUploadFileContents(validUploadFileTest1, armEod.getId(), caseDocument.getChecksum()));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(caseDocument.getCreatedDateTime());

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        ExternalObjectDirectoryEntity foundAnnotationEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(ARM_RPO_PENDING.getId(), foundAnnotationEod.getStatus().getId());
        assertEquals("e7cde7c6-15d7-4c7e-a85d-a468c7ea72b9", foundAnnotationEod.getExternalFileId());
        assertEquals("1cf976c7-cedd-703f-ab70-01588bd56d50", foundAnnotationEod.getExternalRecordId());
        assertTrue(foundAnnotationEod.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken);
        verify(armDataManagementApi).listResponseBlobs(hashcode1);

        verify(armDataManagementApi).getBlobData(createRecordFilename1);
        verify(armDataManagementApi).getBlobData(uploadFileFilename1);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename1);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename1);

        verify(armDataManagementApi).deleteBlobData(blobNameAndPath1);
    }

    @Test
    void batchProcessResponseFiles_WithCaseDocumentInvalidResponseFilenames() throws IOException {

        // given
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimalBuilder().caseNumber("Case1")
            .courthouse(dartsDatabase.getCourthouseStub().createCourthouseUnlessExists("Bristol")).build();
        courtCaseEntity = dartsPersistence.save(courtCaseEntity);
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        CaseDocumentEntity caseDocument = PersistableFactory.getCaseDocumentTestData()
            .someMinimalBuilder().checksum("xC3CCA7021CF79B42F245AF350601C284").courtCase(courtCaseEntity)
            .lastModifiedById(uploadedBy.getId())
            .build();
        caseDocument.setFileName("test_case_document.docx");

        caseDocument = dartsPersistence.save(caseDocument);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().caseDocument(caseDocument).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID().toString()).checksum("xC3CCA7021CF79B42F245AF350601C284").build();

        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        armEod = dartsPersistence.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_invalid_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("dropzone/DARTS/response/%s_04e6bc3b-952a-79b6-8362-13259aae1895_invalid_1_uf.rsp", hashcode1);
        String createRecordFilename2 = String.format("dropzone/DARTS/response/%s__a17b9015-e6ad-77c5-8d1e-13259aae1896_cr.rsp", hashcode1);
        String invalidLineFileFilename2 = String.format("dropzone/DARTS/response/%s__a17b9015-e6ad-77c5-8d1e-13259aae1896_il.rsp", hashcode1);
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
        BinaryData invalidLineFileBinaryDataTest2 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest2, 1234));
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

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(caseDocument.getCreatedDateTime());

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        ExternalObjectDirectoryEntity foundAnnotationEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(ARM_DROP_ZONE.getId(), foundAnnotationEod.getStatus().getId());
        assertFalse(foundAnnotationEod.isResponseCleaned());
    }

    @Test
    void batchProcessResponseFiles_WithCaseDocumentInvalidResponseFilenamesNoEod() throws IOException {

        // given
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntity, uploadedBy);
        caseDocument.setFileName("test_case_document.docx");
        dartsDatabase.save(caseDocument);

        String manifest1Uuid = UUID.randomUUID().toString();

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_invalid_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("dropzone/DARTS/response/%s_04e6bc3b-952a-79b6-8362-13259aae1895_invalid_1_uf.rsp", hashcode1);
        String createRecordFilename2 = String.format("dropzone/DARTS/response/%s_b17b9015-e6ad-77c5-8d1e-13259aae1896_0_cr.rsp", hashcode1);
        String invalidLineFileFilename2 = String.format("dropzone/DARTS/response/%s_c17b9015-e6ad-77c5-8d1e-13259aae1896_1_il.rsp", hashcode1);
        String uploadFileFilename3 = String.format("dropzone/DARTS/response/%s_d17b9015-e6ad-77c5-8d1e-13259aae1896_1_uf.rsp", hashcode1);
        String invalidLineFileFilename3 = String.format("dropzone/DARTS/response/%s_e17b9015-e6ad-77c5-8d1e-13259aae1896_0_il.rsp", hashcode1);
        List<String> hashcodeResponses = List.of(createRecordFilename1, uploadFileFilename1,
                                                 createRecordFilename2, invalidLineFileFilename2,
                                                 uploadFileFilename3, invalidLineFileFilename3);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses).thenReturn(Collections.emptyList());

        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String validUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
        String createRecordFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String invalidLineFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";
        String validUploadFileTest3 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
        String invalidLineFileTest3 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";

        int externalObjectDirectoryId1 = 5678;
        int externalObjectDirectoryId2 = 1234;
        int externalObjectDirectoryId3 = 7788;
        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, externalObjectDirectoryId1));
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(
            getUploadFileContents(validUploadFileTest1, externalObjectDirectoryId1, caseDocument.getChecksum()));
        BinaryData createRecordBinaryDataTest2 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest2, externalObjectDirectoryId2));
        BinaryData invalidLineFileBinaryDataTest2 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest2, externalObjectDirectoryId2));
        BinaryData uploadFileBinaryDataTest3 = convertStringToBinaryData(getUploadFileContents(
            validUploadFileTest3, externalObjectDirectoryId3, "123"));
        BinaryData invalidLineFileBinaryDataTest3 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest3, externalObjectDirectoryId3));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);
        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenReturn(createRecordBinaryDataTest2);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename2)).thenReturn(invalidLineFileBinaryDataTest2);
        when(armDataManagementApi.getBlobData(uploadFileFilename3)).thenReturn(uploadFileBinaryDataTest3);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename3)).thenReturn(invalidLineFileBinaryDataTest3);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(caseDocument.getCreatedDateTime());

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        Optional<ExternalObjectDirectoryEntity> notFoundCaseEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(externalObjectDirectoryId1);
        assertTrue(notFoundCaseEod.isEmpty());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken);
        verify(armDataManagementApi, times(2)).listResponseBlobs(hashcode1);

        verify(armDataManagementApi).getBlobData(createRecordFilename2);
        verify(armDataManagementApi).getBlobData(invalidLineFileFilename2);

        verify(armDataManagementApi).getBlobData(uploadFileFilename3);
        verify(armDataManagementApi).getBlobData(invalidLineFileFilename3);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename2);
        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename2);

        verify(armDataManagementApi).deleteBlobData(uploadFileFilename3);
        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename3);

        verify(armDataManagementApi).deleteBlobData(blobNameAndPath1);
    }

    @Test
    void batchProcessResponseFiles_WithResponseAsInvalidManifestFile() throws IOException {

        // given
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimalBuilder().caseNumber("Case1")
            .courthouse(dartsDatabase.getCourthouseStub().createCourthouseUnlessExists("Bristol")).build();
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        courtCaseEntity = dartsPersistence.save(courtCaseEntity);

        CaseDocumentEntity caseDocument = PersistableFactory.getCaseDocumentTestData()
            .someMinimalBuilder().courtCase(courtCaseEntity)
            .lastModifiedById(uploadedBy.getId()).build();
        caseDocument.setFileName("test_case_document.docx");
        caseDocument = dartsPersistence.save(caseDocument);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().caseDocument(caseDocument).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID().toString()).build();

        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        armEod = dartsPersistence.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename2 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_1_cr.rsp", hashcode1);
        String invalidLineFileFilename2 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1896_0_il.rsp", hashcode1);
        List<String> hashcodeResponses = List.of(createRecordFilename2, invalidLineFileFilename2);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses);

        String createRecordFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String invalidLineFileTest2 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/InvalidLineFile.rsp";

        BinaryData createRecordBinaryDataTest2 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest2, armEod.getId()));
        BinaryData invalidLineFileBinaryDataTest2 = convertStringToBinaryData(getInvalidLineFileContents(invalidLineFileTest2, armEod.getId()));

        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenReturn(createRecordBinaryDataTest2);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename2)).thenReturn(invalidLineFileBinaryDataTest2);

        when(armDataManagementApi.deleteBlobData(createRecordFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename2)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(caseDocument.getCreatedDateTime());

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        ExternalObjectDirectoryEntity foundAnnotationEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(ARM_RESPONSE_MANIFEST_FAILED.getId(), foundAnnotationEod.getStatus().getId());
        assertTrue(foundAnnotationEod.isResponseCleaned());
        assertNotNull(foundAnnotationEod.getErrorCode());
    }

    @Test
    void batchProcessResponseFiles_WithErrorCodeInResponse() throws IOException {

        // given
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimalBuilder().caseNumber("Case1")
            .courthouse(dartsDatabase.getCourthouseStub().createCourthouseUnlessExists("Bristol")).build();
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        CaseDocumentEntity caseDocument = PersistableFactory.getCaseDocumentTestData()
            .someMinimalBuilder().courtCase(courtCaseEntity)
            .lastModifiedById(uploadedBy.getId()).build();
        caseDocument.setFileName("test_case_document.docx");
        caseDocument = dartsPersistence.save(caseDocument);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().caseDocument(caseDocument).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID().toString()).build();

        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        armEod = dartsPersistence.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
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

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(caseDocument.getCreatedDateTime());

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        ExternalObjectDirectoryEntity foundCaseDocEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(ARM_RESPONSE_PROCESSING_FAILED.getId(), foundCaseDocEod.getStatus().getId());
        assertTrue(foundCaseDocEod.isResponseCleaned());
        assertNotNull(foundCaseDocEod.getErrorCode());
    }

    @Test
    void batchProcessResponseFiles_ReturnsNoRecordsToProcess() {

        // given
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimal();

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);

        String manifest1Uuid = UUID.randomUUID().toString();

        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .verificationAttempts(1).externalLocation(UUID.randomUUID().toString()).build();
        armEod1.setTransferAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        dartsPersistence.save(armEod1);

        List<String> blobNamesAndPaths = new ArrayList<>();
        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void batchProcessResponseFiles_ThrowsExceptionWhenListingPrefix() {

        // given
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimal();

        OffsetDateTime startTime = OffsetDateTime.parse(T_13_00_00_Z);
        OffsetDateTime endTime = OffsetDateTime.parse(T_13_45_00_Z);
        MediaEntity media1 = createMediaEntity(hearing, startTime, endTime, 1);

        String manifest1Uuid = UUID.randomUUID().toString();

        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod1 = PersistableFactory
            .getExternalObjectDirectoryTestData().someMinimalBuilder().media(media1).status(dartsDatabase
                                                                                                .getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
        armEod1.setTransferAttempts(1);
        armEod1.setManifestFile(manifestFile1);
        dartsPersistence.save(armEod1);

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenThrow(new AzureException());

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(1, foundMedia.getVerificationAttempts());
        assertFalse(foundMedia.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void batchProcessResponseFiles_WithNullTranscriptionChecksum() throws IOException {

        // given
        authorisationStub.givenTestSchema();
        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();

        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        TranscriptionDocumentEntity transcriptionDocumentEntity = PersistableFactory.getTranscriptionDocument()
            .someMinimalBuilder().transcription(transcriptionEntity)
            .fileName(fileName).fileType(fileType).fileSize(fileSize).uploadedBy(testUser).build();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        transcriptionDocumentEntity = dartsPersistence.save(transcriptionDocumentEntity);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().transcriptionDocumentEntity(transcriptionDocumentEntity).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();

        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        armEod = dartsPersistence.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
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

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(transcriptionEntity.getEndTime());

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        ExternalObjectDirectoryEntity foundTranscriptionEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId(), foundTranscriptionEod.getStatus().getId());
        assertTrue(foundTranscriptionEod.isResponseCleaned());
    }

    @Test
    void batchProcessResponseFiles_WithNullAnnotationChecksum() throws IOException {

        // given
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        String testAnnotation = "TestAnnotation";
        AnnotationEntity annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(testUser, testAnnotation);

        when(userIdentity.getUserAccount()).thenReturn(testUser);
        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = OffsetDateTime.now();
        AnnotationDocumentEntity annotationDocument = PersistableFactory.getAnnotationDocumentTestData()
            .someMinimalBuilder().fileSize(fileSize).fileName(fileName).fileType(fileType)
            .uploadedBy(testUser).uploadedDateTime(uploadedDateTime).annotation(annotation).build();

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifestFile1 = prefix() + "_" + manifest1Uuid + ".a360";

        ExternalObjectDirectoryEntity armEod = PersistableFactory
            .getExternalObjectDirectoryTestData().someMinimalBuilder().annotationDocumentEntity(annotationDocument)
            .status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .verificationAttempts(1).transferAttempts(1)
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();

        armEod.setTransferAttempts(1);
        armEod.setManifestFile(manifestFile1);
        armEod = dartsPersistence.save(armEod);

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
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

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(annotationDocument.getUploadedDateTime());

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        ExternalObjectDirectoryEntity foundAnnotationEod = dartsDatabase.getExternalObjectDirectoryRepository()
            .findById(armEod.getId()).orElseThrow();
        assertEquals(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId(), foundAnnotationEod.getStatus().getId());
        assertTrue(foundAnnotationEod.isResponseCleaned());
        verify(armDataManagementApi).deleteBlobData(blobNameAndPath1);
    }

    @Test
    void batchProcessResponseFiles_WithAnnotationNoEodFound() throws IOException {

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

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/%s_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", prefix(), manifest1Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementApi.listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken)).thenReturn(continuationTokenBlobs);
        String hashcode1 = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename1 = String.format("dropzone/DARTS/response/%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", hashcode1);
        String uploadFileFilename1 = String.format("dropzone/DARTS/response/%s_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp", hashcode1);
        List<String> hashcodeResponses = getHashcodeResponses(hashcode1, createRecordFilename1, uploadFileFilename1);

        when(armDataManagementApi.listResponseBlobs(hashcode1)).thenReturn(hashcodeResponses).thenReturn(Collections.emptyList());

        String createRecordFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        String validUploadFileTest1 = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";

        int eodId = 1234;
        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTest1, eodId));
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(
            getUploadFileContents(validUploadFileTest1, eodId, annotationDocument.getChecksum()));

        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);

        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(blobNameAndPath1)).thenReturn(true);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(annotationDocument.getUploadedDateTime());

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        Optional<ExternalObjectDirectoryEntity> notFoundAnnotationEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(eodId);
        assertTrue(notFoundAnnotationEod.isEmpty());

        verify(armDataManagementApi).listResponseBlobsUsingMarker(prefix(), BATCH_SIZE, continuationToken);
        verify(armDataManagementApi, times(2)).listResponseBlobs(hashcode1);

        verify(armDataManagementApi).getBlobData(createRecordFilename1);
        verify(armDataManagementApi).getBlobData(uploadFileFilename1);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename1);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename1);

        verify(armDataManagementApi).deleteBlobData(blobNameAndPath1);
    }

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

        ExternalObjectDirectoryEntity armEod1 = PersistableFactory.getExternalObjectDirectoryTestData().someMinimalBuilder()
            .media(media1).status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM)).externalLocation(UUID.randomUUID().toString()).build();
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

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, externalObjectDirectoryEntities.size());
        ExternalObjectDirectoryEntity foundEod = externalObjectDirectoryEntities.getFirst();
        assertEquals("2023-06-10T14:08:28.316382Z", foundEod.getInputUploadProcessedTs().toString());
        assertEquals(ARM_MISSING_RESPONSE.getId(), foundEod.getStatus().getId());
    }

    protected MediaEntity createMediaEntity(HearingEntity hearing, OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return dartsPersistence.save(
            PersistableFactory.getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                startTime,
                endTime,
                channel
            ));

    }

    protected BinaryData convertStringToBinaryData(String contents) {
        return BinaryData.fromString(contents);
    }

    protected String getInputUploadFileContents(String inputUploadFile) throws IOException {
        return getContentsFromFile(inputUploadFile);
    }

    protected String getInvalidLineFileContents(String invalidLineFilename, Integer externalObjectDirectoryId) throws IOException {
        String expectedResponse = getContentsFromFile(invalidLineFilename);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(externalObjectDirectoryId));
        return expectedResponse;
    }

    protected String getUploadFileContents(String uploadFilename, int externalObjectDirectoryId, String checksum) throws IOException {
        String expectedResponse = getContentsFromFile(uploadFilename);
        expectedResponse = expectedResponse.replaceAll("<CHECKSUM>", checksum);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(externalObjectDirectoryId));
        return expectedResponse;
    }

    protected String getCreateRecordFileContents(String createRecordFilename, Integer externalObjectDirectoryId) throws IOException {
        String expectedResponse = getContentsFromFile(createRecordFilename);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(externalObjectDirectoryId));
        return expectedResponse;
    }
}