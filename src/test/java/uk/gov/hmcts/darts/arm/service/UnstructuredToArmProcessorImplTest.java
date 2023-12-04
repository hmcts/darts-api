package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;
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
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmProcessorImpl;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnstructuredToArmProcessorImplTest {

    public static final String TEST_BINARY_DATA = "test binary data";

    private static final Integer EXAMPLE_ARM_ENTITY_ID = 100;
    private static final Integer EXAMPLE_MEDIA_ID = 20;
    private static final Integer EXAMPLE_TRANSCRIPTION_ID = 50;
    private static final Integer EXAMPLE_ANNOTATION_ID = 70;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private MediaEntity mediaEntity;
    @Mock
    private TranscriptionDocumentEntity transcriptionDocumentEntity;
    @Mock
    private AnnotationDocumentEntity annotationDocumentEntity;
    @Mock
    ExternalObjectDirectoryEntity externalObjectDirectoryEntityUnstructured;
    @Mock
    ExternalObjectDirectoryEntity externalObjectDirectoryEntityArm;
    @Mock
    ExternalLocationTypeEntity externalLocationTypeUnstructured;
    @Mock
    ExternalLocationTypeEntity externalLocationTypeArm;
    private UnstructuredToArmProcessor unstructuredToArmProcessor;
    @Mock
    ObjectDirectoryStatusEntity objectDirectoryStatusEntityStored;
    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        unstructuredToArmProcessor = new UnstructuredToArmProcessorImpl(externalObjectDirectoryRepository,
                                                                        objectDirectoryStatusRepository, externalLocationTypeRepository,
                                                                        dataManagementApi, armDataManagementApi, userAccountRepository);
    }

    @Test
    void processUnstructuredToArmMedia() {
        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);
        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityUnstructured));
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(objectDirectoryStatusEntityStored,
                                                                                         objectDirectoryStatusEntityStored,
                                                                                         externalLocationTypeUnstructured,
                                                                                         externalLocationTypeArm)).thenReturn(inboundList);

        when(externalObjectDirectoryEntityArm.getMedia().getId()).thenReturn(EXAMPLE_MEDIA_ID);
        when(externalObjectDirectoryEntityArm.getTranscriptionDocumentEntity().getId()).thenReturn(EXAMPLE_TRANSCRIPTION_ID);
        when(externalObjectDirectoryEntityArm.getAnnotationDocumentEntity().getId()).thenReturn(EXAMPLE_ANNOTATION_ID);

        when(dataManagementApi.getBlobDataFromUnstructuredContainer(any())).thenReturn(binaryData);
        when(unstructuredToArmProcessor.generateFilename(externalObjectDirectoryEntityArm)).thenReturn("100_10_1");

        unstructuredToArmProcessor.processUnstructuredToArm();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());


    }

}
