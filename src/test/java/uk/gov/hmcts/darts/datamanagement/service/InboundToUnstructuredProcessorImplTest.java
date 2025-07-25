package uk.gov.hmcts.darts.datamanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.datamanagement.service.impl.InboundToUnstructuredProcessorImpl;
import uk.gov.hmcts.darts.task.config.InboundToUnstructuredAutomatedTaskConfig;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundToUnstructuredProcessorImplTest {
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private InboundToUnstructuredProcessorSingleElement singleElementProcessor;
    @Mock
    private InboundToUnstructuredAutomatedTaskConfig asyncTaskConfig;

    private InboundToUnstructuredProcessor inboundToUnstructuredProcessor;

    @BeforeEach
    void setUp() {
        inboundToUnstructuredProcessor = new InboundToUnstructuredProcessorImpl(externalObjectDirectoryRepository,
                                                                                objectRecordStatusRepository,
                                                                                externalLocationTypeRepository,
                                                                                singleElementProcessor,
                                                                                asyncTaskConfig);
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("email", "integrationtest.user@example.com")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void processInboundToUnstructured_ContinuesProcessingNextIterationOnException() throws InterruptedException {
        when(asyncTaskConfig.getThreads()).thenReturn(20);
        when(asyncTaskConfig.getAsyncTimeout()).thenReturn(Duration.ofMinutes(5));
        // given
        when(externalObjectDirectoryRepository.findEodsForTransfer(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(1L, 2L));

        doThrow(new RuntimeException("some exception"))
            .doNothing()
            .when(singleElementProcessor).processSingleElement(any());

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured(100);

        // then
        verify(singleElementProcessor, times(2)).processSingleElement(any());
    }

    @Test
    void processInboundToUnstructured_shouldStopProcessingAndReturn_WhenThrowingInterruptedExceptionOnSecondItem() throws InterruptedException {
        // given
        when(asyncTaskConfig.getThreads()).thenReturn(20);
        when(asyncTaskConfig.getAsyncTimeout()).thenReturn(Duration.ofMinutes(5));
        when(externalObjectDirectoryRepository.findEodsForTransfer(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(1L, 2L));

        doNothing().when(singleElementProcessor).processSingleElement(1L);
        // Simulate InterruptedException
        doAnswer(invocation -> {
            throw new InterruptedException("Simulated interruption");
        }).when(singleElementProcessor).processSingleElement(2L);

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured(100);

        // then
        verify(singleElementProcessor).processSingleElement(1L);
        verify(singleElementProcessor).processSingleElement(2L);
        // Check to ensure that the processor stops processing after the InterruptedException
        verifyNoMoreInteractions(singleElementProcessor);
    }
}
