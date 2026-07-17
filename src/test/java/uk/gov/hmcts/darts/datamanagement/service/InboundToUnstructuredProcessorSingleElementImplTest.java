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
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.impl.InboundToUnstructuredProcessorSingleElementImpl;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus.COMPLETE;
import static uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus.PENDING;

@ExtendWith(MockitoExtension.class)
class InboundToUnstructuredProcessorSingleElementImplTest {

    private static final Long INBOUND_ID = 5555L;
    public static final String UUID_REGEX = "([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})";
    private static final String EXTERNAL_LOCATION_UUID = UUID.randomUUID().toString();
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
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseRetentionRepository caseRetentionRepository;
    @Mock
    private MediaLinkedCaseRepository mediaLinkedCaseRepository;
    private InboundToUnstructuredProcessorSingleElementImpl inboundToUnstructuredProcessor;
    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;
    @Mock
    private AnnotationDocumentEntity annotationDocumentEntity;
    @Mock
    private CaseDocumentEntity caseDocumentEntity;
    @Mock
    private MediaEntity mediaEntity;
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
                                                                                             externalObjectDirectoryRepository,
                                                                                             caseRepository,
                                                                                              caseRetentionRepository,
                                                                                              mediaLinkedCaseRepository);
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
        verify(dataManagementService).copyBlobData(
            eq(INBOUND_CONTAINER_NAME), eq(UNSTRUCTURED_CONTAINER_NAME), eq(EXTERNAL_LOCATION_UUID), matches(UUID_REGEX));
    }

    @Test
    void processInboundToUnstructuredCaseDocument() {

        when(externalObjectDirectoryEntityInbound.getCaseDocument()).thenReturn(caseDocumentEntity);
        when(externalObjectDirectoryEntityInbound.getExternalLocation()).thenReturn(EXTERNAL_LOCATION_UUID);
        when(caseDocumentEntity.getId()).thenReturn(44L);

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        verify(dataManagementService).copyBlobData(
            eq(INBOUND_CONTAINER_NAME), eq(UNSTRUCTURED_CONTAINER_NAME), eq(EXTERNAL_LOCATION_UUID), matches(UUID_REGEX));

    }

    @Test
    void processSingleElement_resetRetentionProcessingForFirstLinkedCase_allLinkedCasesClosedAndLatestRetentionComplete() {
        CourtCaseEntity firstLinkedCase = createCourtCase(1, true);
        CourtCaseEntity secondLinkedCase = createCourtCase(2, true);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityInbound.getExternalLocation()).thenReturn(EXTERNAL_LOCATION_UUID);
        when(mediaEntity.getId()).thenReturn(44L);
        when(mediaLinkedCaseRepository.findByMediaOrderByCourtCaseIdAsc(mediaEntity)).thenReturn(List.of(
            createMediaLinkedCase(firstLinkedCase),
            createMediaLinkedCase(secondLinkedCase)
        ));
        when(caseRetentionRepository.findTopByCourtCaseOrderByCreatedDateTimeDesc(firstLinkedCase)).thenReturn(Optional.of(createCaseRetention(COMPLETE.name())));
        when(caseRetentionRepository.findTopByCourtCaseOrderByCreatedDateTimeDesc(secondLinkedCase)).thenReturn(Optional.of(createCaseRetention(COMPLETE.name())));

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        assertEquals(true, firstLinkedCase.isRetentionUpdated());
        assertEquals(0, firstLinkedCase.getRetentionRetries());
        assertEquals(false, secondLinkedCase.isRetentionUpdated());
        verify(caseRepository).save(firstLinkedCase);
    }

    @Test
    void processSingleElement_doNotResetRetentionProcessing_linkedCaseOpen() {
        CourtCaseEntity firstLinkedCase = createCourtCase(1, true);
        CourtCaseEntity openLinkedCase = createCourtCase(2, false);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityInbound.getExternalLocation()).thenReturn(EXTERNAL_LOCATION_UUID);
        when(mediaEntity.getId()).thenReturn(44L);
        when(mediaLinkedCaseRepository.findByMediaOrderByCourtCaseIdAsc(mediaEntity)).thenReturn(List.of(
            createMediaLinkedCase(firstLinkedCase),
            createMediaLinkedCase(openLinkedCase)
        ));
        when(caseRetentionRepository.findTopByCourtCaseOrderByCreatedDateTimeDesc(firstLinkedCase)).thenReturn(Optional.of(createCaseRetention(COMPLETE.name())));

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(caseRepository, never()).save(firstLinkedCase);
    }

    @Test
    void processSingleElement_doNotResetRetentionProcessing_latestLinkedCaseRetentionPending() {
        CourtCaseEntity firstLinkedCase = createCourtCase(1, true);
        CourtCaseEntity pendingRetentionCase = createCourtCase(2, true);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityInbound.getExternalLocation()).thenReturn(EXTERNAL_LOCATION_UUID);
        when(mediaEntity.getId()).thenReturn(44L);
        when(mediaLinkedCaseRepository.findByMediaOrderByCourtCaseIdAsc(mediaEntity)).thenReturn(List.of(
            createMediaLinkedCase(firstLinkedCase),
            createMediaLinkedCase(pendingRetentionCase)
        ));
        when(caseRetentionRepository.findTopByCourtCaseOrderByCreatedDateTimeDesc(firstLinkedCase)).thenReturn(Optional.of(createCaseRetention(COMPLETE.name())));
        when(caseRetentionRepository.findTopByCourtCaseOrderByCreatedDateTimeDesc(pendingRetentionCase)).thenReturn(Optional.of(createCaseRetention(PENDING.name())));

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(caseRepository, never()).save(firstLinkedCase);
    }

    @Test
    void processingThrowsExceptionIfInboundObjectIsNotFound() {

        when(externalObjectDirectoryRepository.findById(INBOUND_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                     () -> inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID));
    }

    private CourtCaseEntity createCourtCase(Integer caseId, boolean closed) {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setId(caseId);
        courtCase.setClosed(closed);
        courtCase.setRetentionUpdated(false);
        courtCase.setRetentionRetries(3);
        return courtCase;
    }

    private MediaLinkedCaseEntity createMediaLinkedCase(CourtCaseEntity courtCase) {
        MediaLinkedCaseEntity mediaLinkedCase = new MediaLinkedCaseEntity();
        mediaLinkedCase.setMedia(mediaEntity);
        mediaLinkedCase.setCourtCase(courtCase);
        return mediaLinkedCase;
    }

    private CaseRetentionEntity createCaseRetention(String currentState) {
        CaseRetentionEntity caseRetention = new CaseRetentionEntity();
        caseRetention.setCurrentState(currentState);
        return caseRetention;
    }

}