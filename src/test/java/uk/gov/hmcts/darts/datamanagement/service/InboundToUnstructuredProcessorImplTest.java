package uk.gov.hmcts.darts.datamanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.datamanagement.service.impl.InboundToUnstructuredProcessorImpl;
import uk.gov.hmcts.darts.task.config.InboundToUnstructuredAutomatedTaskConfig;
import uk.gov.hmcts.darts.util.AsyncUtil;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    }

    @Test
    void processInboundToUnstructured_ContinuesProcessingNextIterationOnException() {
        when(asyncTaskConfig.getThreads()).thenReturn(20);
        when(asyncTaskConfig.getAsyncTimeout()).thenReturn(Duration.ofMinutes(5));
        // given
        ExternalObjectDirectoryEntity eod1 = new ExternalObjectDirectoryEntity();
        eod1.setId(1L);
        ExternalObjectDirectoryEntity eod2 = new ExternalObjectDirectoryEntity();
        eod2.setId(2L);
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
    void processInboundToUnstructured_throwsInterruptedException() {
        // given
        ExternalObjectDirectoryEntity eod1 = new ExternalObjectDirectoryEntity();
        eod1.setId(1L);
        ExternalObjectDirectoryEntity eod2 = new ExternalObjectDirectoryEntity();
        eod2.setId(2L);
        when(externalObjectDirectoryRepository.findEodsForTransfer(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(1L, 2L));

        try (MockedStatic<AsyncUtil> mockedStatic = mockStatic(AsyncUtil.class)) {
            // Mock the static method call to throw InterruptedException
            mockedStatic.when(() -> AsyncUtil.invokeAllAwaitTermination(anyList(), any(InboundToUnstructuredAutomatedTaskConfig.class)))
                .thenThrow(new InterruptedException("Mocked InterruptedException"));

            // when
            inboundToUnstructuredProcessor.processInboundToUnstructured(100);

        } catch (Exception e) {
            // then
            assertEquals("Mocked InterruptedException", e.getMessage());
        }
    }
}
