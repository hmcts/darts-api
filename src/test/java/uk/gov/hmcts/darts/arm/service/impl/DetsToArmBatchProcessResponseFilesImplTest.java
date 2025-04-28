package uk.gov.hmcts.darts.arm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.blobs.ArmResponseBatchData;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseCreateRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseInputUploadFileRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseInvalidLineRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.service.DeleteArmResponseFilesHelper;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.CreateRecordFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.InvalidLineFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetsToArmBatchProcessResponseFilesImplTest {
    public static final String UPLOAD_RESPONSE_TIMESTAMP_FORAMT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS[XXXX][XXXXX]";
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ExternalObjectDirectoryService externalObjectDirectoryService;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private LogApi logApi;
    @Mock
    private DetsDataManagementConfiguration configuration;
    @Mock
    private ObjectStateRecordRepository osrRepository;
    @Mock
    private CurrentTimeHelper timeHelper;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private DeleteArmResponseFilesHelper deleteArmResponseFilesHelper;

    private DetsToArmBatchProcessResponseFilesImpl detsToArmBatchProcessResponseFilesImpl;

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    private ExternalObjectDirectoryEntity externalObjectDirectoryEntity;
    private ObjectStateRecordEntity objectStateRecordEntity;
    private BatchInputUploadFileFilenameProcessor batchInputUploadFileFilenameProcessor;
    private CreateRecordFilenameProcessor createRecordFilenameProcessor;
    private UploadFileFilenameProcessor uploadFileFilenameProcessor;
    private InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor;

    @AfterAll
    public static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @BeforeEach
    void setUp() {
        detsToArmBatchProcessResponseFilesImpl = new DetsToArmBatchProcessResponseFilesImpl(
            externalObjectDirectoryRepository,
            armDataManagementApi,
            fileOperationService,
            armDataManagementConfiguration,
            objectMapper,
            userIdentity,
            timeHelper,
            externalObjectDirectoryService,
            logApi,
            deleteArmResponseFilesHelper,
            configuration,
            osrRepository
        );

        String batchMetadataFilename = "dropzone/DARTS/response/DETS_a17b9015-e6ad-77c5-8d1e-13259aae1895_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        batchInputUploadFileFilenameProcessor = new BatchInputUploadFileFilenameProcessor(batchMetadataFilename);
        String createRecordFilename = "dropzone/DARTS/response/6a374f19a9ce7dc9cc480ea8d4eca0fb_12374f19a9ce7dc9cc480ea8d4eca0fb_1_cr.rsp";
        createRecordFilenameProcessor = new CreateRecordFilenameProcessor(createRecordFilename);
        String uploadFileFilename = "dropzone/DARTS/response/6a374f19a9ce7dc9cc480ea8d4eca0fb_45674f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        uploadFileFilenameProcessor = new UploadFileFilenameProcessor(uploadFileFilename);
        String invalidLineFileFilename = "dropzone/DARTS/response/6a374f19a9ce7dc9cc480ea8d4eca0fb_78974f19a9ce7dc9cc480ea8d4eca0fb_1_il.rsp";
        invalidLineFileFilenameProcessor = new InvalidLineFileFilenameProcessor(invalidLineFileFilename);

        externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setId(1L);
        externalObjectDirectoryEntity.setStatus(EodHelper.armProcessingResponseFilesStatus());
        externalObjectDirectoryEntity.setVerificationAttempts(1);
        objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(123L);
        externalObjectDirectoryEntity.setOsrUuid(objectStateRecordEntity.getUuid());

    }

    @Test
    void getManifestFilePrefix() {
        // given
        when(configuration.getDetsManifestFilePrefix()).thenReturn("DETS");

        // when
        String result = detsToArmBatchProcessResponseFilesImpl.getManifestFilePrefix();

        // then
        assertNotNull(result);
        verify(configuration).getDetsManifestFilePrefix();
    }

    @Test
    void preProcessResponseFilesActions() {
        // given
        when(osrRepository.findByArmEodId(externalObjectDirectoryEntity.getId())).thenReturn(Optional.of(objectStateRecordEntity));

        // when
        detsToArmBatchProcessResponseFilesImpl.preProcessResponseFilesActions(externalObjectDirectoryEntity.getId());

        // then
        verify(osrRepository).findByArmEodId(1);
        verify(osrRepository).save(objectStateRecordEntity);
    }

    @Test
    void onUploadFileChecksumValidationSuccess() {
        // given
        ArmResponseUploadFileRecord uploadFileRecord = new ArmResponseUploadFileRecord();
        uploadFileRecord.setA360FileId("a360FileId");
        String checksum = "checksum";
        uploadFileRecord.setMd5(checksum);
        uploadFileRecord.setFileSize(100);
        ArmResponseCreateRecord armResponseCreateRecord = new ArmResponseCreateRecord();

        ArmResponseBatchData batchData = ArmResponseBatchData.builder()
            .externalObjectDirectoryId(externalObjectDirectoryEntity.getId())
            .armResponseCreateRecord(armResponseCreateRecord)
            .createRecordFilenameProcessor(createRecordFilenameProcessor)
            .armResponseUploadFileRecord(uploadFileRecord)
            .uploadFileFilenameProcessor(uploadFileFilenameProcessor)
            .build();
        when(osrRepository.findByArmEodId(anyLong())).thenReturn(Optional.of(objectStateRecordEntity));
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        // when
        detsToArmBatchProcessResponseFilesImpl.onUploadFileChecksumValidationSuccess(batchInputUploadFileFilenameProcessor,
                                                                                     batchData,
                                                                                     uploadFileRecord,
                                                                                     externalObjectDirectoryEntity,
                                                                                     checksum,
                                                                                     userAccount);

        // then
        verify(osrRepository).findByArmEodId(1L);
        verify(osrRepository).save(objectStateRecordEntity);
    }

    @Test
    void onUploadFileChecksumValidationFailure() {
        // given
        ArmResponseUploadFileRecord uploadFileRecord = new ArmResponseUploadFileRecord();
        uploadFileRecord.setA360FileId("a360FileId");
        uploadFileRecord.setMd5("invalidchecksum");
        when(osrRepository.findByArmEodId(anyLong())).thenReturn(Optional.of(objectStateRecordEntity));
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        // when
        detsToArmBatchProcessResponseFilesImpl
            .onUploadFileChecksumValidationFailure(uploadFileRecord, externalObjectDirectoryEntity, "checksum", userAccount);

        // then
        verify(osrRepository).findByArmEodId(1);
        verify(osrRepository).save(objectStateRecordEntity);
    }

    @Test
    void processUploadFileDataFailure() {
        // given
        ArmResponseUploadFileRecord uploadFileRecord = mock(ArmResponseUploadFileRecord.class);
        UploadFileFilenameProcessor processor = mock(UploadFileFilenameProcessor.class);
        when(osrRepository.findByArmEodId(anyLong())).thenReturn(Optional.of(objectStateRecordEntity));
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        // when
        detsToArmBatchProcessResponseFilesImpl.processUploadFileDataFailure(uploadFileRecord, processor, externalObjectDirectoryEntity, userAccount);

        // then
        verify(osrRepository).findByArmEodId(1L);
        verify(osrRepository).save(objectStateRecordEntity);
    }

    @Test
    void processInvalidLineFileActions() {
        // given
        ArmResponseInvalidLineRecord invalidLineRecord = new ArmResponseInvalidLineRecord();
        invalidLineRecord.setErrorStatus("errorStatus");
        invalidLineRecord.setExceptionDescription("exceptionDescription");
        invalidLineRecord.setInput(
            """
                {\\\"operation\\\":\\\"create_record\\\",\\\"relation_id\\\":\\\"1\\\",\\\"record_metadata\\\":{\\\"record_class\\\":\\\"A360TEST\\\",
                \\\"publisher\\\":\\\"A360\\\",\\\"region\\\":\\\"GBR\\\",\\\"title\\\":\\\"CGITestFilesMalformedManifest_1.00MB_100.00MB_001\\\",
                \\\"recordDate\\\":\\\"2023-12-21T10:03:53Z\\\"}}
                """);

        when(osrRepository.findByArmEodId(anyLong())).thenReturn(Optional.of(objectStateRecordEntity));
        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        // when
        detsToArmBatchProcessResponseFilesImpl.processInvalidLineFileActions(invalidLineRecord, externalObjectDirectoryEntity, userAccount);

        // then
        verify(osrRepository).findByArmEodId(1L);
        verify(osrRepository).save(objectStateRecordEntity);
    }

    @Test
    void getInputUploadFileTimestamp_shouldReturnParsedOffsetDateTime() {
        // given
        String timestamp = "2023-06-10T14:08:28.316382+00:00";
        ArmResponseInputUploadFileRecord inputUploadFileRecord = new ArmResponseInputUploadFileRecord();
        inputUploadFileRecord.setTimestamp(timestamp);

        when(armDataManagementConfiguration.getInputUploadResponseTimestampFormat()).thenReturn(UPLOAD_RESPONSE_TIMESTAMP_FORAMT);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(UPLOAD_RESPONSE_TIMESTAMP_FORAMT);

        // when
        OffsetDateTime result = detsToArmBatchProcessResponseFilesImpl.getInputUploadFileTimestamp(inputUploadFileRecord);

        // then
        assertEquals(OffsetDateTime.parse(timestamp, formatter), result);
    }

    @Test
    void getInputUploadFileTimestamp_shouldThrowIllegalArgumentException_whenTimestampIsInvalid() {
        // given
        String invalidTimestamp = "invalid-timestamp";
        ArmResponseInputUploadFileRecord inputUploadFileRecord = new ArmResponseInputUploadFileRecord();
        inputUploadFileRecord.setTimestamp(invalidTimestamp);

        when(armDataManagementConfiguration.getInputUploadResponseTimestampFormat()).thenReturn(UPLOAD_RESPONSE_TIMESTAMP_FORAMT);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            detsToArmBatchProcessResponseFilesImpl.getInputUploadFileTimestamp(inputUploadFileRecord);
        });
    }

}