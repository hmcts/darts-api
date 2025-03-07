package uk.gov.hmcts.darts.arm.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.DetsToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.DetsToArmPushAutomatedTaskConfig;
import uk.gov.hmcts.darts.test.common.FileStore;
import uk.gov.hmcts.darts.testutils.ExternalObjectDirectoryTestData;
import uk.gov.hmcts.darts.util.AsyncUtil;
import uk.gov.hmcts.darts.util.LogUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
@Slf4j
class DetsToArmBatchPushProcessorImplTest {

    private static final Integer MAX_RETRY_ATTEMPTS = 3;
    private static final String DETS_UUID = UUID.randomUUID().toString();
    public static final long OSR_UUID = 987L;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    private ExternalObjectDirectoryEntity externalObjectDirectoryEntityDets;

    @Mock
    private ArchiveRecordService archiveRecordService;
    @Mock
    private LogApi logApi;

    @Mock
    private DetsToArmProcessorConfiguration detsToArmProcessorConfiguration;
    @Mock
    private ObjectStateRecordRepository objectStateRecordRepository;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private ExternalObjectDirectoryService externalObjectDirectoryService;
    @Mock
    private DetsToArmPushAutomatedTaskConfig detsToArmPushAutomatedTaskConfig;

    @InjectMocks
    private DataStoreToArmHelper dataStoreToArmHelper;

    private DetsToArmBatchPushProcessorImpl detsToArmBatchPushProcessor;

    @TempDir
    private File tempDirectory;

    private ObjectStateRecordEntity objectStateRecordEntity;

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks(false);


    @BeforeEach
    void setUp() throws IOException {
        lenient().when(detsToArmPushAutomatedTaskConfig.getThreads()).thenReturn(1);
        lenient().when(detsToArmPushAutomatedTaskConfig.getAsyncTimeout()).thenReturn(Duration.ofMinutes(5));
        detsToArmBatchPushProcessor = new DetsToArmBatchPushProcessorImpl(
            archiveRecordService,
            dataStoreToArmHelper,
            userIdentity,
            logApi,
            armDataManagementConfiguration,
            externalObjectDirectoryRepository,
            armDataManagementApi,
            detsToArmProcessorConfiguration,
            objectStateRecordRepository,
            currentTimeHelper,
            externalObjectDirectoryService,
            detsToArmPushAutomatedTaskConfig
        );

        lenient().when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(MAX_RETRY_ATTEMPTS);

        lenient().when(detsToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(5);
        String fileLocation = tempDirectory.getAbsolutePath();
        lenient().when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        lenient().when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        MediaEntity mediaEntity1 = new MediaEntity();
        externalObjectDirectoryEntityDets = new ExternalObjectDirectoryTestData().createExternalObjectDirectory(
            mediaEntity1,
            ExternalLocationTypeEnum.DETS,
            ObjectRecordStatusEnum.STORED,
            DETS_UUID);
        externalObjectDirectoryEntityDets.setOsrUuid(OSR_UUID);

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityArm = new ExternalObjectDirectoryTestData()
            .createExternalObjectDirectory(
                mediaEntity1,
                ExternalLocationTypeEnum.ARM,
                ObjectRecordStatusEnum.ARM_INGESTION,
                UUID.randomUUID().toString());
        externalObjectDirectoryEntityArm.setId(345);
        externalObjectDirectoryEntityArm.setStatus(EodHelper.armIngestionStatus());
        objectStateRecordEntity = createMaxObjectStateRecordEntity(888L,
                                                                   externalObjectDirectoryEntityDets.getId(),
                                                                   externalObjectDirectoryEntityArm.getId());
        externalObjectDirectoryEntityDets.setOsrUuid(objectStateRecordEntity.getUuid());
        lenient().when(objectStateRecordRepository.findById(objectStateRecordEntity.getUuid()))
            .thenReturn(Optional.of(objectStateRecordEntity));

        String filename = String.format("DETS_%s.a360", DETS_UUID);
        File manifestFile = new File(fileLocation, filename);
        String content = "Test data";
        try (BufferedWriter fileWriter = Files.newBufferedWriter(manifestFile.toPath());
             PrintWriter printWriter = new PrintWriter(fileWriter)) {
            printWriter.write(content);
        }
        externalObjectDirectoryEntityArm.setManifestFile(manifestFile.getName());

        lenient().when(detsToArmProcessorConfiguration.getManifestFilePrefix()).thenReturn("DETS");
    }

    @AfterEach
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void clean() throws Exception {
        FileStore.getFileStore().remove();

        try (var filesStream = Files.list(tempDirectory.toPath())) {
            log.info("Files left: {}", filesStream.count());
        }
    }

    @Test
    void processDetsToArm_SetObjectStatusNoMatchingDetsRecordErrorMessage() {
        //given
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(emptyList());
        when(detsToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(10);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), eq(200)))
            .thenReturn(List.of(123));
        when(externalObjectDirectoryRepository.findAllById(List.of(123))).thenReturn(List.of(externalObjectDirectoryEntityDets));
        EOD_HELPER_MOCKS.simulateInitWithMockedData();
        //Before this method was made async EOD_HELPER_MOCKS.givenIsEqualLocationReturns(true); was used to enforce the ELT to match
        //This is no longer possible as mockito static mocks don't work well with threads. By setting ELt to ARM it simulates this behavior
        externalObjectDirectoryEntityDets.setExternalLocationType(EOD_HELPER_MOCKS.getArmLocation());

        //when
        detsToArmBatchPushProcessor.processDetsToArm(200);

        // then
        assertTrue(
            objectStateRecordEntity
                .getObjectStatus()
                .startsWith(
                    "Error during batch push DETS EOD 123 to ARM - " +
                        "uk.gov.hmcts.darts.common.exception.DartsException: " +
                        "Unable to find matching external object directory"));

    }

    @Test
    void processDetsToArm_noEodsForTransfer(CapturedOutput output) {
        EOD_HELPER_MOCKS.simulateInitWithMockedData();
        detsToArmBatchPushProcessor = spy(detsToArmBatchPushProcessor);
        doReturn(new ArrayList<>()).when(detsToArmBatchPushProcessor).getDetsEodEntitiesToSendToArm(any(), any(), anyInt());
        // given
        detsToArmBatchPushProcessor.processDetsToArm(5);
        // when
        LogUtil.assertOutputHasMessage(output, "No DETS EODs to process", 5);
    }

    @Test
    void processDetsToArm_asyncException(CapturedOutput output) {
        EOD_HELPER_MOCKS.simulateInitWithMockedData();
        detsToArmBatchPushProcessor = spy(detsToArmBatchPushProcessor);
        doReturn(List.of(1)).when(detsToArmBatchPushProcessor).getDetsEodEntitiesToSendToArm(any(), any(), anyInt());

        try (MockedStatic<AsyncUtil> asyncUtilMockedStatic = Mockito.mockStatic(AsyncUtil.class)) {
            asyncUtilMockedStatic.when(() -> AsyncUtil.invokeAllAwaitTermination(any(), any()))
                .thenThrow(new RuntimeException("Test exception"));
            detsToArmBatchPushProcessor.processDetsToArm(5);
            LogUtil.waitUntilMessage(output, "DETS to ARM batch unexpected exception", 5);

            assertThat(output)
                .contains("DETS to ARM batch unexpected exception")
                .contains("DetsToArmBatchPushProcessorImpljava.lang.RuntimeException: Test exception");
        }
    }

    @Disabled("This test is failing randomly. Ticket raised to fix this issue")
    @Test
    void processDetsToArm_emptyList(CapturedOutput output) {
        // given
        EOD_HELPER_MOCKS.simulateInitWithMockedData();
        detsToArmBatchPushProcessor = spy(detsToArmBatchPushProcessor);
        doReturn(new ArrayList<>()).when(detsToArmBatchPushProcessor).getDetsEodEntitiesToSendToArm(any(), any(), anyInt());

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        LogUtil.assertOutputHasMessage(output, "No DETS EODs to process", 10);
    }

    private ObjectStateRecordEntity createMaxObjectStateRecordEntity(Long uuid, int detsEodId, int armEodId) {
        ObjectStateRecordEntity objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(uuid);
        objectStateRecordEntity.setEodId(String.valueOf(detsEodId));
        objectStateRecordEntity.setArmEodId(String.valueOf(armEodId));
        objectStateRecordEntity.setParentObjectId("ParentObject123");
        objectStateRecordEntity.setContentObjectId("ContentObject123");
        objectStateRecordEntity.setIdClip("Clip123");
        objectStateRecordEntity.setDetsLocation("DetsLocation123");
        objectStateRecordEntity.setFlagFileTransferToDets(false);
        objectStateRecordEntity.setFlagFileAvScanPass(false);
        objectStateRecordEntity.setFlagFileTransfToarml(false);
        objectStateRecordEntity.setFlagFileMfstCreated(false);
        objectStateRecordEntity.setFlagMfstTransfToArml(false);
        objectStateRecordEntity.setFlagRspnRecvdFromArml(false);
        objectStateRecordEntity.setFlagFileIngestStatus(false);
        objectStateRecordEntity.setFlagFileDetsCleanupStatus(false);
        return objectStateRecordEntity;
    }
}