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
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.util.AsyncUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnstructuredToArmBatchProcessorExceptionsTest {

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
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
    @Mock
    private UserAccountEntity testUser;

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

        when(manifestFilePath.toFile()).thenReturn(manifestFile);
        verifyNoMoreInteractions(logApi);

        lenient().when(unstructuredToArmProcessorConfiguration.getThreads()).thenReturn(2);

        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("email", "integrationtest.user@example.com")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        when(userIdentity.getUserAccount()).thenReturn(testUser);

    }

    @Test
    void processUnstructuredToArm_shouldHandleInterruptedExceptionFromTask() {
        // Given
        EOD_HELPER_MOCKS.simulateInitWithMockedData();
        when(unstructuredToArmHelper.getEodEntitiesToSendToArm(any(), any(), anyInt()))
            .thenReturn(List.of(1L, 2L, 3L));
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(2);

        // Mock task to throw InterruptedException
        try (var mockedStatic = mockStatic(AsyncUtil.class)) {
            mockedStatic.when(() -> AsyncUtil.invokeAllAwaitTermination(anyList(), any(UnstructuredToArmProcessorConfiguration.class)))
                .thenAnswer(invocation -> {
                    List<Callable<Void>> tasks = invocation.getArgument(0);
                    for (Callable<Void> task : tasks) {
                        task.call();
                    }
                    return null;
                });

            // When
            unstructuredToArmBatchProcessor.processUnstructuredToArm(5);

            // Then
            verify(logApi, never()).armPushSuccessful(anyLong());
        }
    }

    @Test
    void processUnstructuredToArm_shouldHandleInterruptedExceptionFromAsyncUtil() {
        // Given
        when(unstructuredToArmHelper.getEodEntitiesToSendToArm(any(), any(), anyInt()))
            .thenReturn(List.of(1L, 2L, 3L));
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(2);

        // Mock AsyncUtil to throw InterruptedException
        try (var mockedStatic = mockStatic(AsyncUtil.class)) {
            mockedStatic.when(() -> AsyncUtil.invokeAllAwaitTermination(anyList(), any(UnstructuredToArmProcessorConfiguration.class)))
                .thenThrow(new InterruptedException("Mocked InterruptedException"));

            // When & Then
            InterruptedException exception = assertThrows(InterruptedException.class,
                                                          () -> unstructuredToArmBatchProcessor.processUnstructuredToArm(5));

            // Verify the exception message
            assertEquals("Mocked InterruptedException", exception.getMessage());

            // Verify logging and method behavior
            mockedStatic.verify(() -> AsyncUtil.invokeAllAwaitTermination(anyList(), any(UnstructuredToArmProcessorConfiguration.class)));
            verify(logApi, never()).armPushSuccessful(anyLong());
        }
    }

    @Test
    void processUnstructuredToArm_shouldHandleRuntimeExceptionFromTask() {
        // Given
        when(unstructuredToArmHelper.getEodEntitiesToSendToArm(any(), any(), anyInt()))
            .thenReturn(List.of(1L, 2L, 3L));
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(2);

        // Mock task to throw RuntimeException
        try (var mockedStatic = mockStatic(AsyncUtil.class)) {
            mockedStatic.when(() -> AsyncUtil.invokeAllAwaitTermination(anyList(), any(UnstructuredToArmProcessorConfiguration.class)))
                .thenAnswer(invocation -> {
                    List<Callable<Void>> tasks = invocation.getArgument(0);
                    for (Callable<Void> task : tasks) {
                        throw new RuntimeException("Mocked RuntimeException");
                    }
                    return null;
                });

            // When
            unstructuredToArmBatchProcessor.processUnstructuredToArm(5);

            // Then
            mockedStatic.verify(() -> AsyncUtil.invokeAllAwaitTermination(anyList(), any(UnstructuredToArmProcessorConfiguration.class)));
            verify(logApi, never()).armPushSuccessful(anyLong());
        }
    }
}
