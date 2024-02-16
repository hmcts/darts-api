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

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class InboundToUnstructuredProcessorImplTest {
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    private InboundToUnstructuredProcessor inboundToUnstructuredProcessor;
    @Mock
    ExternalObjectDirectoryEntity externalObjectDirectoryEntityInbound;
    @Mock
    ExternalObjectDirectoryEntity externalObjectDirectoryEntityInbound2;
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
        List<ExternalObjectDirectoryEntity> emptyUnstructuredStored = Collections.emptyList();
            List<ExternalObjectDirectoryEntity> emptyUnstructuredFailed = Collections.emptyList();
        when(externalObjectDirectoryRepository.findByStatusAndType(any(), any()))
            .thenReturn(List.of(externalObjectDirectoryEntityInbound, externalObjectDirectoryEntityInbound2))
            .thenReturn(emptyUnstructuredStored);
        when(externalObjectDirectoryRepository.findByStatusIdInAndType(any(), any()))
            .thenReturn(emptyUnstructuredFailed);
        doThrow(new RuntimeException("some exception"))
            .doNothing()
            .when(singleElementProcessor).processSingleElement(any(), any(), any());

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured();

        // then
        verify(singleElementProcessor, times(2)).processSingleElement(any(), any(), any());
    }

}
