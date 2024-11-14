package uk.gov.hmcts.darts.arm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.arm.service.impl.ArmBatchProcessResponseFilesImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("VariableDeclarationUsageDistance")
class ArmBatchProcessResponseFilesImplTest {

    public static final String PREFIX = "DARTS";
    public static final String RESPONSE_FILENAME_EXTENSION = "a360";
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

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    private ArmBatchProcessResponseFilesImpl armBatchProcessResponseFiles;

    private static final Integer BATCH_SIZE = 2;

    @BeforeEach
    void setupData() {

        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();

        armBatchProcessResponseFiles = new ArmBatchProcessResponseFilesImpl(
            externalObjectDirectoryRepository,
            armDataManagementApi,
            fileOperationService,
            armDataManagementConfiguration,
            objectMapper,
            userIdentity,
            currentTimeHelper,
            externalObjectDirectoryService,
            logApi
        );

    }

    @AfterAll
    public static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @Test
    void batchProcessResponseFilesWithBatchSizeTwo() {

        // given
        String continuationToken = null;

        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(PREFIX);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(RESPONSE_FILENAME_EXTENSION);

        String manifest1Uuid = UUID.randomUUID().toString();
        String manifest2Uuid = UUID.randomUUID().toString();

        String manifestFile1 = "DARTS_" + manifest1Uuid + ".a360";
        String manifestFile2 = "DARTS_" + manifest1Uuid + ".a360";

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
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryArmDropZone));

        when(externalObjectDirectoryRepository.findAllByStatusAndManifestFile(any(), any()))
            .thenReturn(inboundList);

        // when
        armBatchProcessResponseFiles.processResponseFiles(BATCH_SIZE);

        // then
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armDropZoneStatus(), manifestFile1);
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armProcessingResponseFilesStatus(), manifestFile1);
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armDropZoneStatus(), manifestFile2);
        verify(externalObjectDirectoryRepository).findAllByStatusAndManifestFile(EodHelper.armProcessingResponseFilesStatus(), manifestFile2);

        verifyNoMoreInteractions(logApi);
    }
}