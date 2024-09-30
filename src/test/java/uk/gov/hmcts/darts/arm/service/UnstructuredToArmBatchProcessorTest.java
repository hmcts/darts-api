package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.UnstructuredToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmBatchProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;


@ExtendWith(MockitoExtension.class)
class UnstructuredToArmBatchProcessorTest {

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private ExternalObjectDirectoryEntity eod1;
    @Mock
    private ExternalObjectDirectoryEntity eod2;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ArchiveRecordService archiveRecordService;

    private UnstructuredToArmBatchProcessor unstructuredToArmBatchProcessor;
    @Mock
    private ExternalObjectDirectoryService eodService;
    @Mock
    private ArchiveRecordFileGenerator archiveRecordFileGenerator;
    @InjectMocks
    private DataStoreToArmHelper unstructuredToArmHelper;

    @Mock
    UnstructuredToArmProcessorConfiguration unstructuredToArmProcessorConfiguration;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Path manifestFilePath;
    @Mock
    private File manifestFile;
    @Mock
    private LogApi logApi;

    @Mock
    private EodHelper eodHelper;

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    @AfterAll
    static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @BeforeEach
    void setUp() throws IOException {

        unstructuredToArmBatchProcessor = new UnstructuredToArmBatchProcessorImpl(
            archiveRecordService,
            archiveRecordFileGenerator,
            unstructuredToArmHelper,
            userIdentity,
            logApi,
            armDataManagementConfiguration,
            externalObjectDirectoryRepository,
            fileOperationService,
            armDataManagementApi,
            unstructuredToArmProcessorConfiguration,
            eodHelper

        );

        lenient().when(fileOperationService.createFile(any(), any(), anyBoolean())).thenReturn(manifestFilePath);
        when(manifestFilePath.toFile()).thenReturn(manifestFile);
        verifyNoMoreInteractions(logApi);

        ObjectRecordStatusEntity armRawFailedStatus = new ObjectRecordStatusEntity();
        armRawFailedStatus.setId(ARM_RAW_DATA_FAILED.getId());
        armRawFailedStatus.setDescription(ARM_RAW_DATA_FAILED.name());
        lenient().when(objectRecordStatusRepository.getReferenceById(ARM_RAW_DATA_FAILED.getId())).thenReturn(armRawFailedStatus);
        ObjectRecordStatusEntity armManifestFailedStatus = new ObjectRecordStatusEntity();
        armManifestFailedStatus.setId(ARM_MANIFEST_FAILED.getId());
        armManifestFailedStatus.setDescription(ARM_MANIFEST_FAILED.name());
        lenient().when(objectRecordStatusRepository.getReferenceById(ARM_MANIFEST_FAILED.getId())).thenReturn(armManifestFailedStatus);
    }

    @Test
    void testDartsArmClientConfigInBatchQuery() {
        ExternalObjectDirectoryEntity eod10 = new ExternalObjectDirectoryEntity();
        eod10.setId(10);
        eod10.setExternalLocationType(EodHelper.armLocation());
        eod10.setStatus(EodHelper.failedArmManifestFileStatus());
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("darts");
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(any(), any(), any(), any())).thenReturn(List.of(eod10));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(emptyList());
        EOD_HELPER_MOCKS.givenIsEqualLocationReturns(true);
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(10);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        //when
        unstructuredToArmBatchProcessor.processUnstructuredToArm(200);

        //then
        verify(externalObjectDirectoryRepository).findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            EodHelper.armLocation(), 199
        );

        verify(logApi).armPushFailed(anyInt());
    }

    @Test
    void testDetsArmClientConfigInBatchQuery() {
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("dets");
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(emptyList());
        //when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(5);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        EOD_HELPER_MOCKS.givenIsEqualLocationReturns(true);

        //when
        unstructuredToArmBatchProcessor.processUnstructuredToArm(5);

        //then
        verify(externalObjectDirectoryRepository).findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            EodHelper.armLocation(), 5
        );

    }

    @Test
    void testUnknownArmClientConfigInBatchQuery() {
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("unknown");

        //then
        assertThrows(DartsException.class, () -> unstructuredToArmBatchProcessor.processUnstructuredToArm(5));
    }

    @Test
    void testPaginatedBatchQuery() throws IOException {
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("darts");
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(any(), any(), any(), any())).thenReturn(List.of(eod1, eod2));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(emptyList());
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(100);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        when(fileOperationService.createFile(any(), any(), anyBoolean())).thenReturn(manifestFilePath);

        //when
        unstructuredToArmBatchProcessor.processUnstructuredToArm(5000);

        //then
        verify(externalObjectDirectoryRepository).findNotFinishedAndNotExceededRetryInStorageLocation(
            any(),
            any(ExternalLocationTypeEntity.class),
            eq(3),
            eq(Pageable.ofSize(5000)));

        verify(externalObjectDirectoryRepository).findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            EodHelper.armLocation(), 4998
        );

        verifyNoMoreInteractions(logApi);
    }

    @Test
    void testManifestFileName() throws IOException {
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("darts");
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn("/temp_workspace");
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(List.of(eod1));
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(1000);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        //when
        unstructuredToArmBatchProcessor.processUnstructuredToArm(5);

        //then
        verify(fileOperationService).createFile(matches("DARTS_.+\\.a360"), eq("/temp_workspace"), eq(true));

        verifyNoMoreInteractions(logApi);
    }

}
