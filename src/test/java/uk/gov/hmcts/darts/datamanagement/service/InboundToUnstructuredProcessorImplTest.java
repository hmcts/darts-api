package uk.gov.hmcts.darts.datamanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.datamanagement.service.impl.InboundToUnstructuredProcessorImpl;
import uk.gov.hmcts.darts.task.config.InboundToUnstructuredAutomatedTaskConfig;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundToUnstructuredProcessorImplTest {
    @Mock
    ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    ExternalLocationTypeRepository externalLocationTypeRepository;
    InboundToUnstructuredProcessor inboundToUnstructuredProcessor;
    @Mock
    InboundToUnstructuredProcessorSingleElement singleElementProcessor;
    @Mock
    InboundToUnstructuredAutomatedTaskConfig asyncTaskConfig;

    @BeforeEach
    void setUp() {
        inboundToUnstructuredProcessor = new InboundToUnstructuredProcessorImpl(externalObjectDirectoryRepository,
                                                                                objectRecordStatusRepository, externalLocationTypeRepository,
                                                                                singleElementProcessor,
                                                                                asyncTaskConfig);
    }

    @Test
    void testContinuesProcessingNextIterationOnException() {
        when(asyncTaskConfig.getThreads()).thenReturn(20);
        when(asyncTaskConfig.getAsyncTimeout()).thenReturn(Duration.ofMinutes(5));
        // given
        ExternalObjectDirectoryEntity eod1 = new ExternalObjectDirectoryEntity();
        eod1.setId(1);
        ExternalObjectDirectoryEntity eod2 = new ExternalObjectDirectoryEntity();
        eod2.setId(2);
        when(externalObjectDirectoryRepository.findEodsForTransfer(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(1, 2));

        doThrow(new RuntimeException("some exception"))
            .doNothing()
            .when(singleElementProcessor).processSingleElement(any());

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured(100);

        // then
        verify(singleElementProcessor, times(2)).processSingleElement(any());
    }

}
