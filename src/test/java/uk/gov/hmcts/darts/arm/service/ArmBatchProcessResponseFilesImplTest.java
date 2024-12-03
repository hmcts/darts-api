package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.blobs.ArmBatchResponses;
import uk.gov.hmcts.darts.arm.model.blobs.ArmResponseBatchData;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.arm.service.impl.ArmBatchProcessResponseFilesImpl;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmBatchProcessResponseFilesImplTest {

    public static final String PREFIX = "DARTS";
    public static final String RESPONSE_FILENAME_EXTENSION = "a360";
    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();
    private static final Integer BATCH_SIZE = 2;

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
    private ObjectStateRecordRepository objectStateRecordRepository;
    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryArmDropZone;

    private ArmBatchProcessResponseFilesImplProtectedMethodSupport armBatchProcessResponseFiles;

    @BeforeEach
    void setupData() {

        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();

        armBatchProcessResponseFiles = spy(new ArmBatchProcessResponseFilesImplProtectedMethodSupport(
            externalObjectDirectoryRepository,
            objectStateRecordRepository,
            armDataManagementApi,
            fileOperationService,
            armDataManagementConfiguration,
            objectMapper,
            userIdentity,
            currentTimeHelper,
            externalObjectDirectoryService,
            logApi
        ));

    }

    @AfterAll
    public static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchProcessResponseFilesWithBatchSizeTwo() throws Exception {

        // given
        final String continuationToken = null;

        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(PREFIX);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(RESPONSE_FILENAME_EXTENSION);
        BinaryData binaryData = mock(BinaryData.class);
        when(armDataManagementApi.getBlobData(any())).thenReturn(binaryData);
        final String bindaryDataString = "{\"timestamp\": \"2021-08-10T10:00:00.000Z\"}";
        when(binaryData.toString()).thenReturn(bindaryDataString);

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
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE);

        // then
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armDropZoneStatus(), manifestFile1);
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armProcessingResponseFilesStatus(), manifestFile1);
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armDropZoneStatus(), manifestFile2);
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armProcessingResponseFilesStatus(), manifestFile2);

        verify(armDataManagementApi).getBlobData(blobNameAndPath1);
        verify(armDataManagementApi).getBlobData(blobNameAndPath2);

        verify(externalObjectDirectoryArmDropZone).setInputUploadProcessedTs(OffsetDateTime.of(2021, 8, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        verify(externalObjectDirectoryEntity2).setInputUploadProcessedTs(OffsetDateTime.of(2021, 8, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        verify(externalObjectDirectoryRepository).saveAll(List.of(externalObjectDirectoryArmDropZone));
        verify(externalObjectDirectoryRepository).saveAll(List.of(externalObjectDirectoryEntity2));
        verifyNoMoreInteractions(logApi);
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchProcessResponseFilesWithBatchSizeTwoWithFailedToUpload() throws Exception {

        // given
        final String continuationToken = null;

        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(PREFIX);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(RESPONSE_FILENAME_EXTENSION);
        BinaryData binaryData1 = mock(BinaryData.class);
        BinaryData binaryData2 = mock(BinaryData.class);

        OffsetDateTime offsetDateTime1 = OffsetDateTime.now().plusDays(1);
        OffsetDateTime offsetDateTime2 = OffsetDateTime.now().minusDays(1);

        when(armDataManagementApi.getBlobData(any())).thenReturn(binaryData1, binaryData2);
        final String bindaryDataString1 = "{\"timestamp\": \"" + offsetDateTime1 + "\"}";
        final String bindaryDataString2 = "{\"timestamp\": \"" + offsetDateTime2 + "\"}";

        when(binaryData1.toString()).thenReturn(bindaryDataString1);
        when(binaryData2.toString()).thenReturn(bindaryDataString2);

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
        when(externalObjectDirectoryEntity2.getInputUploadProcessedTs()).thenReturn(offsetDateTime2);

        when(externalObjectDirectoryRepository.findAllByStatusAndManifestFile(any(), any()))
            .thenReturn(List.of(externalObjectDirectoryEntity1), List.of(externalObjectDirectoryEntity2));

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE);

        // then
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armDropZoneStatus(), manifestFile1);
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armProcessingResponseFilesStatus(), manifestFile1);
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armDropZoneStatus(), manifestFile2);
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armProcessingResponseFilesStatus(), manifestFile2);

        verify(armDataManagementApi).getBlobData(blobNameAndPath1);
        verify(armDataManagementApi).getBlobData(blobNameAndPath2);

        verify(externalObjectDirectoryEntity1).setInputUploadProcessedTs(offsetDateTime1);
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

        ObjectStateRecordEntity objectStateRecordEntity = mock(ObjectStateRecordEntity.class);
        when(externalObjectDirectoryEntity.getObjectStateRecordEntity()).thenReturn(objectStateRecordEntity);
        doNothing().when(armBatchProcessResponseFiles).updateExternalObjectDirectoryStatus(any(), any(), any());


        armBatchProcessResponseFiles.processBatchResponseFiles(batchUploadFileFilenameProcessor,
                                                               armBatchResponses,
                                                               userAccount);

        verify(armBatchResponses).getArmBatchResponseMap();
        verify(armBatchResponseMap).values();
        verify(externalObjectDirectoryEntity, times(2)).getInputUploadProcessedTs();
        verify(externalObjectDirectoryEntity).getObjectStateRecordEntity();
        verify(armBatchProcessResponseFiles).getExternalObjectDirectoryEntity(123);
        verify(currentTimeHelper).currentOffsetDateTime();
        verify(armBatchProcessResponseFiles).updateExternalObjectDirectoryStatus(
            externalObjectDirectoryEntity, EodHelper.armResponseProcessingFailedStatus(),
            userAccount
        );
        verify(objectStateRecordEntity).setObjectStatus("No response files produced by ARM within 1 day");
        verify(objectStateRecordRepository).save(objectStateRecordEntity);
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
        verifyNoInteractions(objectStateRecordRepository);
    }


    class ArmBatchProcessResponseFilesImplProtectedMethodSupport extends ArmBatchProcessResponseFilesImpl {

        public ArmBatchProcessResponseFilesImplProtectedMethodSupport(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                                                      ObjectStateRecordRepository objectStateRecordRepository,
                                                                      ArmDataManagementApi armDataManagementApi, FileOperationService fileOperationService,
                                                                      ArmDataManagementConfiguration armDataManagementConfiguration, ObjectMapper objectMapper,
                                                                      UserIdentity userIdentity,
                                                                      CurrentTimeHelper currentTimeHelper,
                                                                      ExternalObjectDirectoryService externalObjectDirectoryService, LogApi logApi) {
            super(externalObjectDirectoryRepository, objectStateRecordRepository, armDataManagementApi, fileOperationService, armDataManagementConfiguration,
                  objectMapper, userIdentity, currentTimeHelper, externalObjectDirectoryService, logApi);
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