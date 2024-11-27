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
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseInvalidLineRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.CreateRecordFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.InvalidLineFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetsToArmBatchProcessResponseFilesImplTest {
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
        externalObjectDirectoryEntity.setId(1);
        externalObjectDirectoryEntity.setStatus(EodHelper.armProcessingResponseFilesStatus());
        externalObjectDirectoryEntity.setTransferAttempts(1);
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
        verify(configuration, times(1)).getDetsManifestFilePrefix();
    }

    @Test
    void preProcessResponseFilesActions() {
        // given
        when(osrRepository.findByArmEodId(String.valueOf(externalObjectDirectoryEntity.getId()))).thenReturn(Optional.of(objectStateRecordEntity));

        // when
        detsToArmBatchProcessResponseFilesImpl.preProcessResponseFilesActions(externalObjectDirectoryEntity.getId());

        // then
        verify(osrRepository, times(1)).findByArmEodId(String.valueOf(externalObjectDirectoryEntity.getId()));
        verify(osrRepository, times(1)).save(objectStateRecordEntity);
    }

    @Test
    void onUploadFileChecksumValidationSuccess() {
        // given
        ArmResponseUploadFileRecord uploadFileRecord = new ArmResponseUploadFileRecord();
        uploadFileRecord.setA360FileId("a360FileId");
        String checksum = "checksum";
        uploadFileRecord.setMd5(checksum);
        ArmResponseCreateRecord armResponseCreateRecord = new ArmResponseCreateRecord();

        ArmResponseBatchData batchData = ArmResponseBatchData.builder()
            .externalObjectDirectoryId(externalObjectDirectoryEntity.getId())
            .armResponseCreateRecord(armResponseCreateRecord)
            .createRecordFilenameProcessor(createRecordFilenameProcessor)
            .armResponseUploadFileRecord(uploadFileRecord)
            .uploadFileFilenameProcessor(uploadFileFilenameProcessor)
            .build();
        when(osrRepository.findByArmEodId(anyString())).thenReturn(Optional.of(objectStateRecordEntity));

        // when
        detsToArmBatchProcessResponseFilesImpl.onUploadFileChecksumValidationSuccess(batchInputUploadFileFilenameProcessor,
                                                                                     batchData,
                                                                                     uploadFileRecord,
                                                                                     externalObjectDirectoryEntity,
                                                                                     checksum);

        // then
        verify(osrRepository, times(1)).findByArmEodId(anyString());
        verify(osrRepository, times(1)).save(objectStateRecordEntity);
    }

    @Test
    void onUploadFileChecksumValidationFailure() {
        // given
        ArmResponseUploadFileRecord uploadFileRecord = new ArmResponseUploadFileRecord();
        uploadFileRecord.setA360FileId("a360FileId");
        uploadFileRecord.setMd5("invalidchecksum");
        when(osrRepository.findByArmEodId(anyString())).thenReturn(Optional.of(objectStateRecordEntity));

        // when
        detsToArmBatchProcessResponseFilesImpl
            .onUploadFileChecksumValidationFailure(uploadFileRecord, externalObjectDirectoryEntity, "checksum");

        // then
        verify(osrRepository, times(1)).findByArmEodId(anyString());
        verify(osrRepository, times(1)).save(objectStateRecordEntity);
    }

    @Test
    void processUploadFileDataFailure() {
        // given
        ArmResponseUploadFileRecord uploadFileRecord = mock(ArmResponseUploadFileRecord.class);
        UploadFileFilenameProcessor processor = mock(UploadFileFilenameProcessor.class);
        when(osrRepository.findByArmEodId(anyString())).thenReturn(Optional.of(objectStateRecordEntity));

        // when
        detsToArmBatchProcessResponseFilesImpl.processUploadFileDataFailure(uploadFileRecord, processor, externalObjectDirectoryEntity);

        // then
        verify(osrRepository, times(1)).findByArmEodId(anyString());
        verify(osrRepository, times(1)).save(objectStateRecordEntity);
    }

    @Test
    void processInvalidLineFileActions() {
        // given
        ArmResponseInvalidLineRecord invalidLineRecord = new ArmResponseInvalidLineRecord();
        invalidLineRecord.setErrorStatus("errorStatus");
        invalidLineRecord.setExceptionDescription("exceptionDescription");
        invalidLineRecord.setInput(
            "{\\\"operation\\\":\\\"create_record\\\",\\\"relation_id\\\":\\\"<EODID>\\\","
                + "\\\"record_metadata\\\":{\\\"record_class\\\":\\\"A360TEST\\\",\\\"publisher\\\":\\\"A360\\\","
                + "\\\"region\\\":\\\"GBR\\\",\\\"title\\\":\\\"CGITestFilesMalformedManifest_1.00MB_100.00MB_001\\\","
                + "\\\"recordDate\\\":\\\"2023-12-21T10:03:53Z\\\"}}");

        when(osrRepository.findByArmEodId(anyString())).thenReturn(Optional.of(objectStateRecordEntity));

        // when
        detsToArmBatchProcessResponseFilesImpl.processInvalidLineFileActions(invalidLineRecord, externalObjectDirectoryEntity);

        // then
        verify(osrRepository, times(1)).findByArmEodId(anyString());
        verify(osrRepository, times(1)).save(objectStateRecordEntity);
    }

    public ObjectStateRecordEntity createMaxObjectStateRecordEntity(Long uuid, int detsEodId, int armEodId) {
        ObjectStateRecordEntity objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(uuid);
        objectStateRecordEntity.setEodId(String.valueOf(detsEodId));
        objectStateRecordEntity.setArmEodId(String.valueOf(armEodId));
        objectStateRecordEntity.setParentId("Parent123");
        objectStateRecordEntity.setParentObjectId("ParentObject123");
        objectStateRecordEntity.setContentObjectId("ContentObject123");
        objectStateRecordEntity.setObjectType("Type123");
        objectStateRecordEntity.setIdClip("Clip123");
        objectStateRecordEntity.setIdCase("Case123");
        objectStateRecordEntity.setCourthouseName("Courthouse123");
        objectStateRecordEntity.setCasId(123);
        objectStateRecordEntity.setDateLastAccessed(OffsetDateTime.now());
        objectStateRecordEntity.setRelationId("Relation123");
        objectStateRecordEntity.setDetsLocation("DetsLocation123");
        objectStateRecordEntity.setFlagFileTransferToDets(false);
        objectStateRecordEntity.setFlagFileAvScanPass(false);
        objectStateRecordEntity.setFlagFileTransfToarml(false);
        objectStateRecordEntity.setFlagFileMfstCreated(false);
        objectStateRecordEntity.setFlagMfstTransfToArml(false);
        objectStateRecordEntity.setFlagRspnRecvdFromArml(false);
        objectStateRecordEntity.setFlagFileIngestStatus(false);
        objectStateRecordEntity.setFlagFileDetsCleanupStatus(false);
        objectStateRecordEntity.setFlagFileRetainedInOds(false);
        return objectStateRecordEntity;
    }
}