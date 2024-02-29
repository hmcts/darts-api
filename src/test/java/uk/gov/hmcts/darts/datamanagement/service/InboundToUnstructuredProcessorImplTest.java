package uk.gov.hmcts.darts.datamanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.datamanagement.service.impl.InboundToUnstructuredProcessorImpl;

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

    @BeforeEach
    void setUp() {
        inboundToUnstructuredProcessor = new InboundToUnstructuredProcessorImpl(externalObjectDirectoryRepository,
                                                                                objectRecordStatusRepository, externalLocationTypeRepository,
                                                                                singleElementProcessor);
    }

    @Test
    void testContinuesProcessingNextIterationOnException() {
        // given
        when(externalObjectDirectoryRepository.findEodIdsForTransfer(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(1, 2));

        doThrow(new RuntimeException("some exception"))
            .doNothing()
            .when(singleElementProcessor).processSingleElement(any());

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured();

        // then
        verify(singleElementProcessor, times(2)).processSingleElement(any());
    }

}
