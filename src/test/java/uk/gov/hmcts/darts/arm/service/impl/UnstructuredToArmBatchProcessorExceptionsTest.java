package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.UnstructuredToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmBatchProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;

@ExtendWith(MockitoExtension.class)
class UnstructuredToArmBatchProcessorExceptionsTest {

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

    @Mock
    private DataStoreToArmHelper unstructuredToArmHelper;

    @Mock
    private UnstructuredToArmProcessorConfiguration unstructuredToArmProcessorConfiguration;

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

        when(unstructuredToArmProcessorConfiguration.getThreads()).thenReturn(2);

        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("email", "integrationtest.user@example.com")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void processUnstructuredToArm_shouldThrowInterruptedException() {
        //given
        EOD_HELPER_MOCKS.simulateInitWithMockedData();
        when(externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(any(), any(), any(), any())).thenReturn(List.of(12L, 34L));
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorage(any(), any(), any(), any())).thenReturn(emptyList());
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(2);
        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);
        when(unstructuredToArmHelper.getEodEntitiesToSendToArm(any(), any(), anyInt()))
            .thenReturn(List.of(1L, 2L, 3L));
        when(externalObjectDirectoryRepository.findAllById(any())).thenReturn(emptyList());

        // Simulate InterruptedException
        doAnswer(invocation -> {
            throw new InterruptedException("Simulated interruption");
        }).when(unstructuredToArmHelper).copyUnstructuredRawDataToArm(any(), any(), any(), any(), any());

        // when
        unstructuredToArmBatchProcessor.processUnstructuredToArm(5);

        //then
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
