package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.UnstructuredToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmBatchProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.util.AsyncUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
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
    private UserIdentity userIdentity;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ArchiveRecordService archiveRecordService;

    private UnstructuredToArmBatchProcessor unstructuredToArmBatchProcessor;

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

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    @AfterAll
    static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @BeforeEach
    void setUp() throws IOException {

        unstructuredToArmBatchProcessor = new UnstructuredToArmBatchProcessorImpl(
            archiveRecordService,
            unstructuredToArmHelper,
            userIdentity,
            logApi,
            armDataManagementConfiguration,
            externalObjectDirectoryRepository,
            unstructuredToArmProcessorConfiguration
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
    void processUnstructuredToArm_ShouldSucceed_WhereDartsArmClientConfigInBatchQuery() {

        ExternalObjectDirectoryEntity eod10 = new ExternalObjectDirectoryEntity();
        eod10.setId(10L);
        eod10.setExternalLocationType(EodHelper.armLocation());
        eod10.setStatus(EodHelper.failedArmManifestFileStatus());
        //given
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(any(), any(), any(), any())).thenReturn(List.of(10L));
        when(externalObjectDirectoryRepository.findAllById(List.of(10L))).thenReturn(List.of(eod10));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(emptyList());
        EOD_HELPER_MOCKS.simulateInitWithMockedData();
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(10);
        when(unstructuredToArmProcessorConfiguration.getThreads()).thenReturn(20);
        when(unstructuredToArmProcessorConfiguration.getAsyncTimeout()).thenReturn(Duration.of(100, ChronoUnit.SECONDS));
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        //when
        unstructuredToArmBatchProcessor.processUnstructuredToArm(200);

        //then
        verify(externalObjectDirectoryRepository).findEodsNotInOtherStorage(
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            EodHelper.armLocation(), 199
        );

        verify(logApi).armPushFailed(anyLong());
    }

    @Test
    void processUnstructuredToArm_ShouldSucceed_WherePaginatedBatchQuery() {
        //given
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(any(), any(), any(), any())).thenReturn(List.of(12L, 34L));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(emptyList());
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(100);
        when(unstructuredToArmProcessorConfiguration.getThreads()).thenReturn(20);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

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
    void processUnstructuredToArm_throwsInterruptedException() {
        //given
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(any(), any(), any(), any())).thenReturn(List.of(12L, 34L));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(emptyList());
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(100);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        try (MockedStatic<AsyncUtil> mockedStatic = mockStatic(AsyncUtil.class)) {
            // Mock the static method call to throw InterruptedException
            mockedStatic.when(() -> AsyncUtil.invokeAllAwaitTermination(anyList(), any(UnstructuredToArmProcessorConfiguration.class)))
                .thenThrow(new InterruptedException("Mocked InterruptedException"));

            // when
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
    }

    @Test
    void processUnstructuredToArm_shouldThrowInterruptedException() {
        //given
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(any(), any(), any(), any())).thenReturn(List.of(12L, 34L));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(emptyList());
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(2);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);
        when(unstructuredToArmHelper.getEodEntitiesToSendToArm(any(), any(), anyInt()))
            .thenReturn(List.of(1L, 2L, 3L));
        //when(externalObjectDirectoryRepository.findAllById(any())).thenReturn(emptyList());

        // Simulate InterruptedException
        doAnswer(invocation -> {
            throw new InterruptedException("Simulated interruption");
        }).when(unstructuredToArmHelper).copyUnstructuredRawDataToArm(any(), any(), any(), any(), any());

        // when
        try {
            unstructuredToArmBatchProcessor.processUnstructuredToArm(5);
        } catch (Exception e) {
            // Fail the test if the exception is not handled
            fail("InterruptedException was not handled properly");
        }
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

//    @Test
//    void processUnstructuredToArm_shouldHandleInterruptedExceptionFromAsyncUtil() throws Exception {
//        // Mock data
//        when(unstructuredToArmHelper.getEodEntitiesToSendToArm(any(ExternalLocationTypeEntity.class), any(), anyInt()))
//            .thenReturn(List.of(1L, 2L, 3L));
//        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(2);
//
//        // Mock AsyncUtil to throw InterruptedException
//        try (var mockedStatic = mockStatic(AsyncUtil.class)) {
//            mockedStatic.when(() -> AsyncUtil.invokeAllAwaitTermination(anyList(), any(UnstructuredToArmProcessorConfiguration.class)))
//                .thenThrow(new InterruptedException("Mocked InterruptedException"));
//
//            // Execute
//            unstructuredToArmBatchProcessor.processUnstructuredToArm(5);
//
//            // Verify
//            mockedStatic.verify(() -> AsyncUtil.invokeAllAwaitTermination(anyList(), any(UnstructuredToArmProcessorConfiguration.class)));
//            verify(logApi, never()).armPushSuccessful(anyLong());
//        }
//    }
//
//    @Test
//    void processUnstructuredToArm_shouldHandleInterruptedExceptionFromTask() throws Exception {
//        // Mock data
//        when(unstructuredToArmHelper.getEodEntitiesToSendToArm(any(ExternalLocationTypeEntity.class), any(), anyInt()))
//            .thenReturn(List.of(1L, 2L, 3L));
//        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(2);
//
//        // Mock task to throw InterruptedException
//        Callable<Void> mockTask = mock(Callable.class);
//        doThrow(new InterruptedException("Mocked InterruptedException")).when(mockTask).call();
//
//        try (var mockedStatic = mockStatic(AsyncUtil.class)) {
//            mockedStatic.when(() -> AsyncUtil.invokeAllAwaitTermination(anyList(), any(UnstructuredToArmProcessorConfiguration.class)))
//                .thenAnswer(invocation -> {
//                    List<Callable<Void>> tasks = invocation.getArgument(0);
//                    for (Callable<Void> task : tasks) {
//                        task.call();
//                    }
//                    return null;
//                });
//
//            // Execute
//            unstructuredToArmBatchProcessor.processUnstructuredToArm(5);
//
//            // Verify
//            verify(logApi, never()).armPushSuccessful(anyLong());
//        }
//    }
}
