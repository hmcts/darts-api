package uk.gov.hmcts.darts.datamanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.impl.InboundToUnstructuredProcessorSingleElementImpl;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@ExtendWith(MockitoExtension.class)
class InboundToUnstructuredProcessorSingleElementImplTest {

    private static final Integer INBOUND_ID = 5555;
    private static final UUID externalLocationUUID = UUID.randomUUID();
    private static final String INBOUND_CONTAINER_NAME = "darts-inbound-container";
    private static final String UNSTRUCTURED_CONTAINER_NAME = "darts-unstructured";
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
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
    @Mock
    ExternalLocationTypeEntity externalLocationTypeUnstructured;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityAwaiting;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityStored;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        inboundToUnstructuredProcessor = new InboundToUnstructuredProcessorSingleElementImpl(dataManagementService, dataManagementConfiguration,
                                                                                             userAccountRepository, objectRecordStatusRepository,
                                                                                             externalLocationTypeRepository,
                                                                                             externalObjectDirectoryRepository);
        when(externalObjectDirectoryRepository.findById(INBOUND_ID)).thenReturn(Optional.of(externalObjectDirectoryEntityInbound));
        when(dataManagementConfiguration.getInboundContainerName()).thenReturn(INBOUND_CONTAINER_NAME);
        when(dataManagementConfiguration.getUnstructuredContainerName()).thenReturn(UNSTRUCTURED_CONTAINER_NAME);
    }

    @Test
    void processInboundToUnstructuredAnnotation() {

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalObjectDirectoryEntityInbound.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryEntityInbound.getExternalLocation()).thenReturn(externalLocationUUID);
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }

    @Test
    void processInboundToUnstructuredCaseDocument() {

        when(externalObjectDirectoryEntityInbound.getCaseDocument()).thenReturn(caseDocumentEntity);
        when(externalObjectDirectoryEntityInbound.getExternalLocation()).thenReturn(externalLocationUUID);
        when(caseDocumentEntity.getId()).thenReturn(44);
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        verify(dataManagementService).copyBlobData(INBOUND_CONTAINER_NAME, UNSTRUCTURED_CONTAINER_NAME, externalLocationUUID);

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }
}