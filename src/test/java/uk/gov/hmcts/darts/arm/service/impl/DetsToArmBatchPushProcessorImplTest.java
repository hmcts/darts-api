package uk.gov.hmcts.darts.arm.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.DetsToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.DetsToArmBatchPushProcessor;
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
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.FileStore;
import uk.gov.hmcts.darts.testutils.ExternalObjectDirectoryTestData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
class DetsToArmBatchPushProcessorImplTest {

    private static final Integer MAX_RETRY_ATTEMPTS = 3;
    private static final UUID DETS_UUID = UUID.randomUUID();
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
    private FileOperationService fileOperationService;
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

    @InjectMocks
    private DataStoreToArmHelper dataStoreToArmHelper;

    private DetsToArmBatchPushProcessor detsToArmBatchPushProcessor;

    @TempDir
    private File tempDirectory;

    private ObjectStateRecordEntity objectStateRecordEntity;

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    @AfterAll
    static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @BeforeEach
    void setUp() throws IOException {

        detsToArmBatchPushProcessor = new DetsToArmBatchPushProcessorImpl(
            archiveRecordService,
            dataStoreToArmHelper,
            userIdentity,
            logApi,
            armDataManagementConfiguration,
            externalObjectDirectoryRepository,
            fileOperationService,
            armDataManagementApi,
            detsToArmProcessorConfiguration,
            objectStateRecordRepository,
            currentTimeHelper,
            externalObjectDirectoryService
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
                UUID.randomUUID());
        externalObjectDirectoryEntityArm.setId(345);
        externalObjectDirectoryEntityArm.setStatus(EodHelper.armIngestionStatus());
        objectStateRecordEntity = createMaxObjectStateRecordEntity(888L,
                                                                   externalObjectDirectoryEntityDets.getId(),
                                                                   externalObjectDirectoryEntityArm.getId());
        externalObjectDirectoryEntityDets.setOsrUuid(objectStateRecordEntity.getUuid());
        lenient().when(objectStateRecordRepository.findById(objectStateRecordEntity.getUuid())).thenReturn(Optional.of(objectStateRecordEntity));

        String filename = String.format("DETS_%s.a360", DETS_UUID);
        File manifestFile = new File(fileLocation, filename);
        String content = "Test data";
        try (BufferedWriter fileWriter = Files.newBufferedWriter(manifestFile.toPath());
             PrintWriter printWriter = new PrintWriter(fileWriter)) {
            printWriter.write(content);
        }
        externalObjectDirectoryEntityArm.setManifestFile(manifestFile.getName());

        lenient().when(fileOperationService.createFile(any(), any(), anyBoolean())).thenReturn(manifestFile.toPath());
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
    void processDetsToArmSetObjectStatusNoMatchingDetsRecordErrorMessage() {
        //given
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(emptyList());
        EOD_HELPER_MOCKS.givenIsEqualLocationReturns(true);
        EOD_HELPER_MOCKS.givenIsEqualStatusReturns(true);
        when(detsToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(10);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            EodHelper.armLocation(), 200
        )).thenReturn(List.of(123));
        when(externalObjectDirectoryRepository.findAllById(List.of(123))).thenReturn(List.of(externalObjectDirectoryEntityDets));

        //when
        detsToArmBatchPushProcessor.processDetsToArm(200);

        //then
        assertTrue(
            objectStateRecordEntity
                .getObjectStatus()
                .startsWith(
                    "Error during batch push DETS EOD 123 to ARM - " +
                        "uk.gov.hmcts.darts.common.exception.DartsException: " +
                        "Unable to find matching external object directory"));

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