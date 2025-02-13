package uk.gov.hmcts.darts.datamanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.impl.InboundToUnstructuredProcessorSingleElementImpl;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@ExtendWith(MockitoExtension.class)
class InboundToUnstructuredProcessorSingleElementImplTest {

    private static final Integer INBOUND_ID = 5555;
    public static final String UUID_REGEX = "([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})";
    private static final UUID EXTERNAL_LOCATION_UUID = UUID.randomUUID();
    private static final String INBOUND_CONTAINER_NAME = "darts-inbound-container";
    private static final String UNSTRUCTURED_CONTAINER_NAME = "darts-unstructured";
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private DataManagementService dataManagementService;
    @Mock
    private DataManagementConfiguration dataManagementConfiguration;
    @Mock
    private UserAccountRepository userAccountRepository;
    private InboundToUnstructuredProcessorSingleElementImpl inboundToUnstructuredProcessor;
    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;
    @Mock
    private AnnotationDocumentEntity annotationDocumentEntity;
    @Mock
    private CaseDocumentEntity caseDocumentEntity;
    @Mock
    ExternalObjectDirectoryEntity externalObjectDirectoryEntityInbound;

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    @AfterAll
    public static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        inboundToUnstructuredProcessor = new InboundToUnstructuredProcessorSingleElementImpl(dataManagementService, dataManagementConfiguration,
                                                                                             userAccountRepository,
                                                                                             externalObjectDirectoryRepository);
        lenient().when(externalObjectDirectoryRepository.findById(INBOUND_ID)).thenReturn(Optional.of(externalObjectDirectoryEntityInbound));
        lenient().when(dataManagementConfiguration.getInboundContainerName()).thenReturn(INBOUND_CONTAINER_NAME);
        lenient().when(dataManagementConfiguration.getUnstructuredContainerName()).thenReturn(UNSTRUCTURED_CONTAINER_NAME);
    }

    @Test
    void processInboundToUnstructuredAnnotation() {

        when(externalObjectDirectoryEntityInbound.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryEntityInbound.getExternalLocation()).thenReturn(EXTERNAL_LOCATION_UUID);

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }

    @Test
    void processInboundToUnstructuredCaseDocument() {

        when(externalObjectDirectoryEntityInbound.getCaseDocument()).thenReturn(caseDocumentEntity);
        when(externalObjectDirectoryEntityInbound.getExternalLocation()).thenReturn(EXTERNAL_LOCATION_UUID);
        when(caseDocumentEntity.getId()).thenReturn(44);

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        verify(dataManagementService).copyBlobData(
            eq(INBOUND_CONTAINER_NAME), eq(UNSTRUCTURED_CONTAINER_NAME), eq(EXTERNAL_LOCATION_UUID.toString()), matches(UUID_REGEX));

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }

    @Test
    void processingThrowsExceptionIfInboundObjectIsNotFound() {

        when(externalObjectDirectoryRepository.findById(INBOUND_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                     () -> inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID));
    }

}