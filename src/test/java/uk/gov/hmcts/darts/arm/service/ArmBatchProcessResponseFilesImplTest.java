package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.UnstructuredToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.model.blobs.ArmBatchResponses;
import uk.gov.hmcts.darts.arm.model.blobs.ArmResponseBatchData;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.arm.service.impl.ArmBatchProcessResponseFilesImpl;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AsyncTaskConfig;
import uk.gov.hmcts.darts.util.AsyncUtil;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmBatchProcessResponseFilesImplTest {

    private static final String PREFIX = "DARTS";
    private static final String RESPONSE_FILENAME_EXTENSION = "a360";
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
    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();
    private static final Integer BATCH_SIZE = 2;
    private static final String DATETIMEKEY = "<datetimekey>";
    private static final String INPUT_UPLOAD_RESPONSE_DATETIME = "2021-08-01T10:08:28.316382+00:00";

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private LogApi logApi;
    @Mock
    private ExternalObjectDirectoryService externalObjectDirectoryService;
    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryArmDropZone;
    @Mock
    private DeleteArmResponseFilesHelper deleteArmResponseFilesHelper;
    @Mock
    private AsyncTaskConfig asyncTaskConfig;

    private ArmBatchProcessResponseFilesImplProtectedMethodSupport armBatchProcessResponseFiles;

    @BeforeEach
    void setupData() {
        lenient().when(asyncTaskConfig.getThreads()).thenReturn(1);
        lenient().when(asyncTaskConfig.getAsyncTimeout()).thenReturn(Duration.ofSeconds(10));

        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();

        armBatchProcessResponseFiles = spy(new ArmBatchProcessResponseFilesImplProtectedMethodSupport(
            externalObjectDirectoryRepository,
            armDataManagementApi,
            fileOperationService,
            armDataManagementConfiguration,
            objectMapper,
            userIdentity,
            currentTimeHelper,
            externalObjectDirectoryService,
            logApi,
            deleteArmResponseFilesHelper
        ));


    }

    @BeforeAll
    static void beforeAll() {
        EOD_HELPER_MOCKS.simulateInitWithMockedData();
    }

    @AfterAll
    static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchProcessResponseFilesWithBatchSizeTwo() {

        // given
        final String continuationToken = null;
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(PREFIX);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(RESPONSE_FILENAME_EXTENSION);
        when(armDataManagementConfiguration.getInputUploadResponseTimestampFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSSSS[XXXX][XXXXX]");

        BinaryData binaryData = mock(BinaryData.class);
        when(armDataManagementApi.getBlobData(any())).thenReturn(binaryData);
        String inputUploadResponse = INPUT_UPLOAD_RESPONSE.replace(DATETIMEKEY, INPUT_UPLOAD_RESPONSE_DATETIME);
        when(binaryData.toString()).thenReturn(inputUploadResponse);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifest2Uuid = UUID.randomUUID().toString();

        final String manifestFile1 = "DARTS_" + manifest1Uuid + ".a360";
        final String manifestFile2 = "DARTS_" + manifest1Uuid + ".a360";

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DARTS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        String blobNameAndPath2 = String.format("dropzone/DARTS/response/DARTS_%s_7a374f19a9ce7dc9cc480ea8d4eca0fc_1_iu.rsp", manifest2Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);
        blobNamesAndPaths.add(blobNameAndPath2);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementConfiguration.getMaxContinuationBatchSize()).thenReturn(2);
        when(armDataManagementApi.listResponseBlobsUsingMarker(PREFIX, 2, continuationToken)).thenReturn(continuationTokenBlobs);
        List<ExternalObjectDirectoryEntity> inboundList1 = new ArrayList<>(Collections.singletonList(externalObjectDirectoryArmDropZone));
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity2 = mock(ExternalObjectDirectoryEntity.class);
        List<ExternalObjectDirectoryEntity> inboundList2 = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntity2));

        when(externalObjectDirectoryRepository.findAllByStatusAndManifestFile(any(), any()))
            .thenReturn(inboundList1, inboundList2);

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armDropZoneStatus(), manifestFile1);
        verify(externalObjectDirectoryRepository, times(2)).findAllByStatusAndManifestFile(EodHelper.armProcessingResponseFilesStatus(), manifestFile1);
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armDropZoneStatus(), manifestFile2);
        verify(externalObjectDirectoryRepository, times(2)).findAllByStatusAndManifestFile(EodHelper.armProcessingResponseFilesStatus(), manifestFile2);

        verify(armDataManagementApi).getBlobData(blobNameAndPath1);
        verify(armDataManagementApi).getBlobData(blobNameAndPath2);

        OffsetDateTime inputUploadProcessedTs = OffsetDateTime.parse(INPUT_UPLOAD_RESPONSE_DATETIME);
        verify(externalObjectDirectoryArmDropZone).setInputUploadProcessedTs(inputUploadProcessedTs);
        verify(externalObjectDirectoryEntity2).setInputUploadProcessedTs(inputUploadProcessedTs);

        verify(externalObjectDirectoryRepository).saveAll(List.of(externalObjectDirectoryArmDropZone));
        verify(externalObjectDirectoryRepository).saveAll(List.of(externalObjectDirectoryEntity2));
        verifyNoMoreInteractions(logApi);
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchProcessResponseFilesWithBatchSizeTwoWithFailedToUpload() {

        // given
        final String continuationToken = null;

        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(PREFIX);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(RESPONSE_FILENAME_EXTENSION);
        when(armDataManagementConfiguration.getInputUploadResponseTimestampFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSSSS[XXXX][XXXXX]");

        BinaryData binaryData1 = mock(BinaryData.class);
        BinaryData binaryData2 = mock(BinaryData.class);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        String dateTime1 = "2027-08-01T10:08:28.316382+00:00";
        String dateTime2 = INPUT_UPLOAD_RESPONSE_DATETIME;

        when(armDataManagementApi.getBlobData(any())).thenReturn(binaryData1, binaryData2);
        final String inputUploadResponse1 = INPUT_UPLOAD_RESPONSE.replace(DATETIMEKEY, dateTime1);
        final String inputUploadResponse2 = INPUT_UPLOAD_RESPONSE.replace(DATETIMEKEY, dateTime2);

        when(binaryData1.toString()).thenReturn(inputUploadResponse1);
        when(binaryData2.toString()).thenReturn(inputUploadResponse2);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifest2Uuid = UUID.randomUUID().toString();

        final String manifestFile1 = "DARTS_" + manifest1Uuid + ".a360";
        final String manifestFile2 = "DARTS_" + manifest1Uuid + ".a360";

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DARTS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        String blobNameAndPath2 = String.format("dropzone/DARTS/response/DARTS_%s_7a374f19a9ce7dc9cc480ea8d4eca0fc_1_iu.rsp", manifest2Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);
        blobNamesAndPaths.add(blobNameAndPath2);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementConfiguration.getMaxContinuationBatchSize()).thenReturn(2);
        when(armDataManagementApi.listResponseBlobsUsingMarker(PREFIX, 2, continuationToken)).thenReturn(continuationTokenBlobs);

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity1 = mock(ExternalObjectDirectoryEntity.class);

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity2 = mock(ExternalObjectDirectoryEntity.class);
        OffsetDateTime inputUploadProcessedTs2 = OffsetDateTime.parse(dateTime2);
        when(externalObjectDirectoryEntity2.getInputUploadProcessedTs()).thenReturn(inputUploadProcessedTs2);

        when(externalObjectDirectoryRepository.findAllByStatusAndManifestFile(any(), any()))
            .thenReturn(List.of(externalObjectDirectoryEntity1), List.of(externalObjectDirectoryEntity2));

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

        // then
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armDropZoneStatus(), manifestFile1);
        verify(externalObjectDirectoryRepository, times(2)).findAllByStatusAndManifestFile(EodHelper.armProcessingResponseFilesStatus(), manifestFile1);
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armDropZoneStatus(), manifestFile2);
        verify(externalObjectDirectoryRepository, times(2)).findAllByStatusAndManifestFile(EodHelper.armProcessingResponseFilesStatus(), manifestFile2);

        verify(armDataManagementApi).getBlobData(blobNameAndPath1);
        verify(armDataManagementApi).getBlobData(blobNameAndPath2);

        verify(externalObjectDirectoryEntity2, never()).setInputUploadProcessedTs(any());

        verify(externalObjectDirectoryRepository).saveAll(List.of(externalObjectDirectoryEntity1));
        verify(externalObjectDirectoryRepository).saveAll(any());
        verifyNoMoreInteractions(logApi);
    }

    @Test
    void updateExternalObjectDirectoryStatus_ArmMissingResponse() {
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = mock(ExternalObjectDirectoryEntity.class);
        ObjectRecordStatusEntity objectRecordStatusEntityOriginal = mock(ObjectRecordStatusEntity.class);
        when(externalObjectDirectoryEntity.getStatus()).thenReturn(objectRecordStatusEntityOriginal);

        Duration armMissingResponseDuration = Duration.ofHours(24);

        when(externalObjectDirectoryEntity.getId()).thenReturn(123);
        when(armDataManagementConfiguration.getArmMissingResponseDuration()).thenReturn(armMissingResponseDuration);

        ObjectRecordStatusEntity objectRecordStatusEntity = mock(ObjectRecordStatusEntity.class);
        when(objectRecordStatusEntity.getId()).thenReturn(23);
        OffsetDateTime currentTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(currentTime);

        UserAccountEntity userAccount = mock(UserAccountEntity.class);

        armBatchProcessResponseFiles.updateExternalObjectDirectoryStatus(
            externalObjectDirectoryEntity,
            objectRecordStatusEntity,
            userAccount
        );

        verify(logApi).logArmMissingResponse(armMissingResponseDuration, 123);
        verify(externalObjectDirectoryEntity, times(2)).getId();
        verify(armDataManagementConfiguration).getArmMissingResponseDuration();
        verify(currentTimeHelper).currentOffsetDateTime();
        verify(externalObjectDirectoryEntity).setStatus(objectRecordStatusEntity);
        verify(externalObjectDirectoryEntity).setLastModifiedBy(userAccount);
        verify(externalObjectDirectoryEntity).setLastModifiedDateTime(currentTime);
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntity);
    }

    @Test
    @SuppressWarnings("unchecked")
    void processBatchResponseFiles_unableToFindResponseFiles_responseProcessingFailedStatus_outsideAllowedTime() {
        ArmBatchResponses armBatchResponses = mock(ArmBatchResponses.class);
        Map<Integer, ArmResponseBatchData> armBatchResponseMap = mock(Map.class);
        when(armBatchResponses.getArmBatchResponseMap()).thenReturn(armBatchResponseMap);

        List<ArmResponseBatchData> armResponseBatchDataList = new ArrayList<>();
        when(armBatchResponseMap.values()).thenReturn(armResponseBatchDataList);

        final BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor = mock(BatchInputUploadFileFilenameProcessor.class);
        final UserAccountEntity userAccount = mock(UserAccountEntity.class);

        ArmResponseBatchData armResponseBatchData = mock(ArmResponseBatchData.class);
        armResponseBatchDataList.add(armResponseBatchData);
        when(armResponseBatchData.getExternalObjectDirectoryId()).thenReturn(123);

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = mock(ExternalObjectDirectoryEntity.class);
        doReturn(externalObjectDirectoryEntity).when(armBatchProcessResponseFiles).getExternalObjectDirectoryEntity(123);

        OffsetDateTime currentTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(currentTime);
        Duration armMissingResponseDuration = Duration.ofHours(24);
        when(armDataManagementConfiguration.getArmMissingResponseDuration()).thenReturn(armMissingResponseDuration);

        when(externalObjectDirectoryEntity.getInputUploadProcessedTs())
            .thenReturn(currentTime.minus(armMissingResponseDuration).minusMinutes(1));

        doNothing().when(armBatchProcessResponseFiles).updateExternalObjectDirectoryStatus(any(), any(), any());


        armBatchProcessResponseFiles.processBatchResponseFiles(batchUploadFileFilenameProcessor,
                                                               armBatchResponses,
                                                               userAccount);

        verify(armBatchResponses).getArmBatchResponseMap();
        verify(armBatchResponseMap).values();
        verify(externalObjectDirectoryEntity, times(2)).getInputUploadProcessedTs();
        verify(externalObjectDirectoryEntity, never()).getObjectStateRecordEntity();
        verify(armBatchProcessResponseFiles).getExternalObjectDirectoryEntity(123);
        verify(currentTimeHelper).currentOffsetDateTime();
        verify(armBatchProcessResponseFiles).updateExternalObjectDirectoryStatus(
            externalObjectDirectoryEntity, EodHelper.armMissingResponseStatus(),
            userAccount
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void processBatchResponseFiles_unableToFindResponseFiles_responseProcessingFailedStatus_insideAllowedTime() {
        ArmBatchResponses armBatchResponses = mock(ArmBatchResponses.class);
        Map<Integer, ArmResponseBatchData> armBatchResponseMap = mock(Map.class);
        when(armBatchResponses.getArmBatchResponseMap()).thenReturn(armBatchResponseMap);

        List<ArmResponseBatchData> armResponseBatchDataList = new ArrayList<>();
        when(armBatchResponseMap.values()).thenReturn(armResponseBatchDataList);

        final BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor = mock(BatchInputUploadFileFilenameProcessor.class);
        final UserAccountEntity userAccount = mock(UserAccountEntity.class);

        ArmResponseBatchData armResponseBatchData = mock(ArmResponseBatchData.class);
        armResponseBatchDataList.add(armResponseBatchData);
        when(armResponseBatchData.getExternalObjectDirectoryId()).thenReturn(123);

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = mock(ExternalObjectDirectoryEntity.class);
        doReturn(externalObjectDirectoryEntity).when(armBatchProcessResponseFiles).getExternalObjectDirectoryEntity(123);

        OffsetDateTime currentTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(currentTime);
        Duration armMissingResponseDuration = Duration.ofHours(24);
        when(armDataManagementConfiguration.getArmMissingResponseDuration()).thenReturn(armMissingResponseDuration);

        when(externalObjectDirectoryEntity.getInputUploadProcessedTs())
            .thenReturn(currentTime.minusHours(23));
        doNothing().when(armBatchProcessResponseFiles).updateExternalObjectDirectoryStatus(any(), any(), any());

        armBatchProcessResponseFiles.processBatchResponseFiles(batchUploadFileFilenameProcessor,
                                                               armBatchResponses,
                                                               userAccount);

        verify(armBatchResponses).getArmBatchResponseMap();
        verify(armBatchResponseMap).values();
        verify(armBatchProcessResponseFiles).getExternalObjectDirectoryEntity(123);
        verify(currentTimeHelper).currentOffsetDateTime();
        verify(armBatchProcessResponseFiles).updateExternalObjectDirectoryStatus(
            externalObjectDirectoryEntity, EodHelper.armDropZoneStatus(),
            userAccount
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void processResponseFiles_throwsInterruptedException() {

        // given
        final String continuationToken = null;
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(PREFIX);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifest2Uuid = UUID.randomUUID().toString();

        List<String> blobNamesAndPaths = new ArrayList<>();
        String blobNameAndPath1 = String.format("dropzone/DARTS/response/DARTS_%s_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp", manifest1Uuid);
        String blobNameAndPath2 = String.format("dropzone/DARTS/response/DARTS_%s_7a374f19a9ce7dc9cc480ea8d4eca0fc_1_iu.rsp", manifest2Uuid);
        blobNamesAndPaths.add(blobNameAndPath1);
        blobNamesAndPaths.add(blobNameAndPath2);

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder()
            .blobNamesAndPaths(blobNamesAndPaths)
            .build();

        when(armDataManagementConfiguration.getMaxContinuationBatchSize()).thenReturn(2);
        when(armDataManagementApi.listResponseBlobsUsingMarker(PREFIX, 2, continuationToken)).thenReturn(continuationTokenBlobs);

        try (MockedStatic<AsyncUtil> mockedStatic = mockStatic(AsyncUtil.class)) {
            // Mock the static method call to throw InterruptedException
            mockedStatic.when(() -> AsyncUtil.invokeAllAwaitTermination(anyList(), any(UnstructuredToArmProcessorConfiguration.class)))
                .thenThrow(new InterruptedException("Mocked InterruptedException"));

            // when
            armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE, asyncTaskConfig);

            // then
            verifyNoMoreInteractions(logApi);
        }
    }

    class ArmBatchProcessResponseFilesImplProtectedMethodSupport extends ArmBatchProcessResponseFilesImpl {

        public ArmBatchProcessResponseFilesImplProtectedMethodSupport(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                                                      ArmDataManagementApi armDataManagementApi, FileOperationService fileOperationService,
                                                                      ArmDataManagementConfiguration armDataManagementConfiguration, ObjectMapper objectMapper,
                                                                      UserIdentity userIdentity,
                                                                      CurrentTimeHelper currentTimeHelper,
                                                                      ExternalObjectDirectoryService externalObjectDirectoryService, LogApi logApi,
                                                                      DeleteArmResponseFilesHelper deleteArmResponseFilesHelper) {
            super(externalObjectDirectoryRepository, armDataManagementApi, fileOperationService, armDataManagementConfiguration,
                  objectMapper, userIdentity, currentTimeHelper, externalObjectDirectoryService, logApi, deleteArmResponseFilesHelper);
        }

        @Override
        public void updateExternalObjectDirectoryStatus(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                        ObjectRecordStatusEntity objectRecordStatus, UserAccountEntity userAccount) {
            super.updateExternalObjectDirectoryStatus(externalObjectDirectory, objectRecordStatus, userAccount);
        }

        @Override
        public void processBatchResponseFiles(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor,
                                              ArmBatchResponses armBatchResponses,
                                              UserAccountEntity userAccount) {
            super.processBatchResponseFiles(batchUploadFileFilenameProcessor, armBatchResponses, userAccount);
        }

        @Override
        public ExternalObjectDirectoryEntity getExternalObjectDirectoryEntity(Integer eodId) {
            return super.getExternalObjectDirectoryEntity(eodId);
        }
    }
}