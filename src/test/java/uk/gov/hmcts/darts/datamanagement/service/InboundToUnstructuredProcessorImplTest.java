package uk.gov.hmcts.darts.datamanagement.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
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
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
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
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.impl.InboundToUnstructuredProcessorImpl;
import uk.gov.hmcts.darts.transcriptions.config.TranscriptionConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.apache.commons.codec.digest.DigestUtils.md5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_CHECKSUM_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_FILE_NOT_FOUND;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_FILE_SIZE_CHECK_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_FILE_TYPE_CHECK_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class InboundToUnstructuredProcessorImplTest {

    public static final String TEST_MP_2 = "test";
    public static final int MAX_FILE_SIZE_VALID = 100;
    public static final String MP_2 = "mp2";
    public static final String TEST_DOC = "test.doc";
    public static final String DOC = "doc";
    public static final String DOCX = "docx";
    public static final String TEST_BINARY_DATA = "test binary data";
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private DataManagementService dataManagementService;
    @Mock
    private DataManagementConfiguration dataManagementConfiguration;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private TranscriptionConfigurationProperties transcriptionConfigurationProperties;
    @Mock
    private AudioConfigurationProperties audioConfigurationProperties;
    private InboundToUnstructuredProcessor inboundToUnstructuredProcessor;
    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;
    @Mock
    private MediaEntity mediaEntity;
    @Mock
    private TranscriptionDocumentEntity transcriptionDocumentEntity;
    @Mock
    private AnnotationDocumentEntity annotationDocumentEntity;
    @Mock
    ExternalObjectDirectoryEntity externalObjectDirectoryEntityInbound;
    @Mock
    ExternalObjectDirectoryEntity externalObjectDirectoryEntityFailed;
    @Mock
    ExternalLocationTypeEntity externalLocationTypeInbound;
    @Mock
    ExternalLocationTypeEntity externalLocationTypeUnstructured;
    @Mock
    ObjectDirectoryStatusEntity objectDirectoryStatusEntityAwaiting;
    @Mock
    ObjectDirectoryStatusEntity objectDirectoryStatusEntityStored;
    @Mock
    ObjectDirectoryStatusEntity objectDirectoryStatusEntityFailure;
    @Mock
    ObjectDirectoryStatusEntity objectDirectoryStatusEntityFailureFileSize;
    @Mock
    ObjectDirectoryStatusEntity objectDirectoryStatusEntityFailureFileType;
    @Mock
    ObjectDirectoryStatusEntity objectDirectoryStatusEntityFailureChecksum;
    @Mock
    ObjectDirectoryStatusEntity objectDirectoryStatusEntityFailureFileNotFound;
    @Mock
    ObjectDirectoryStatusEntity objectDirectoryStatusEntityFailureArm;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        inboundToUnstructuredProcessor = new InboundToUnstructuredProcessorImpl(externalObjectDirectoryRepository,
                                                                                objectDirectoryStatusRepository, externalLocationTypeRepository,
                                                                                dataManagementService, dataManagementConfiguration, userAccountRepository,
                               transcriptionConfigurationProperties, audioConfigurationProperties);

    }

    @Test
    void processInboundToUnstructuredMedia() {

        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);
        String calculatedChecksum = new String(encodeBase64(md5(binaryData.toBytes())));

        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);

        when(mediaEntity.getMediaFile()).thenReturn(TEST_MP_2);
        when(mediaEntity.getMediaFormat()).thenReturn("mp2");
        when(mediaEntity.getFileSize()).thenReturn((long) binaryData.toString().length());
        when(mediaEntity.getChecksum()).thenReturn(calculatedChecksum);
        when(objectDirectoryStatusEntityStored.getId()).thenReturn(2);
        when(objectDirectoryStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectDirectoryStatusEntityStored.getId()).thenReturn(2);
        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);
        when(objectDirectoryStatusRepository.getReferenceById(9)).thenReturn(objectDirectoryStatusEntityAwaiting);

        setExpectationsForFailedStates();



        when(audioConfigurationProperties.getAllowedExtensions()).thenReturn(List.of(MP_2));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectDirectoryStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectDirectoryStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }

    private void setExpectationsForFailedStates() {
        when(objectDirectoryStatusRepository.getReferenceById(3)).thenReturn(objectDirectoryStatusEntityFailure);
        when(objectDirectoryStatusRepository.getReferenceById(4)).thenReturn(objectDirectoryStatusEntityFailureFileNotFound);
        when(objectDirectoryStatusRepository.getReferenceById(5)).thenReturn(objectDirectoryStatusEntityFailureFileSize);
        when(objectDirectoryStatusRepository.getReferenceById(6)).thenReturn(objectDirectoryStatusEntityFailureFileType);
        when(objectDirectoryStatusRepository.getReferenceById(7)).thenReturn(objectDirectoryStatusEntityFailureChecksum);
        when(objectDirectoryStatusRepository.getReferenceById(8)).thenReturn(objectDirectoryStatusEntityFailureArm);
    }

    @Test
    void processInboundToUnstructuredMediaBlobException() {
        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(objectDirectoryStatusEntityFailureFileNotFound.getId()).thenReturn(4);
        when(objectDirectoryStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);
        when(objectDirectoryStatusRepository.getReferenceById(9)).thenReturn(objectDirectoryStatusEntityAwaiting);
        when(objectDirectoryStatusRepository.getReferenceById(4)).thenReturn(objectDirectoryStatusEntityFailureFileNotFound);
        when(dataManagementService.getBlobData(any(), any())).thenThrow(new BlobStorageException("Blobbed it", null, null));
        setExpectationsForFailedStates();

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectDirectoryStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectDirectoryStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(FAILURE_FILE_NOT_FOUND.getId(), savedStatus.getId());
    }

    @Test
    void processInboundToUnstructuredMediaWithUnstructured() {

        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);
        String calculatedChecksum = new String(encodeBase64(md5(binaryData.toBytes())));

        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);
        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);

        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityFailed.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityFailed.getStatus())
            .thenReturn(objectDirectoryStatusEntityFailureChecksum)
            .thenReturn(objectDirectoryStatusEntityFailureChecksum)
            .thenReturn(objectDirectoryStatusEntityAwaiting)
            .thenReturn(objectDirectoryStatusEntityAwaiting)
            .thenReturn(objectDirectoryStatusEntityStored);
        when(objectDirectoryStatusEntityFailureChecksum.getId()).thenReturn(7);

        when(mediaEntity.getMediaFile()).thenReturn(TEST_MP_2);
        when(mediaEntity.getMediaFormat()).thenReturn("mp2");
        when(mediaEntity.getFileSize()).thenReturn((long) binaryData.toString().length());
        when(mediaEntity.getChecksum()).thenReturn(calculatedChecksum);
        when(objectDirectoryStatusEntityStored.getId()).thenReturn(2);
        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);
        //when(objectDirectoryStatusRepository.getReferenceById(9)).thenReturn(objectDirectoryStatusEntityAwaiting);
        when(audioConfigurationProperties.getAllowedExtensions()).thenReturn(List.of(MP_2));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectDirectoryStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);


        List<ExternalObjectDirectoryEntity> failedList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityFailed));
        when(externalObjectDirectoryRepository.findByFailedAndType(any(), any(),any(),any(),any(),any(),any())).thenReturn(failedList);


        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityActualList = externalObjectDirectoryEntityCaptor.getAllValues();
        ObjectDirectoryStatusEntity savedStatusUnstructured = externalObjectDirectoryEntityActualList.get(1).getStatus();

        assertEquals(STORED.getId(), savedStatusUnstructured.getId());
    }

    @Test
    void processInboundToUnstructuredTranscription() {

        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);
        String calculatedChecksum = new String(encodeBase64(md5(binaryData.toBytes())));

        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);
        when(externalObjectDirectoryEntityInbound.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(transcriptionDocumentEntity.getFileName()).thenReturn(TEST_DOC);
        when(transcriptionDocumentEntity.getFileSize()).thenReturn(binaryData.toString().length());
        when(transcriptionDocumentEntity.getChecksum()).thenReturn(calculatedChecksum);
        when(objectDirectoryStatusEntityStored.getId()).thenReturn(2);
        when(objectDirectoryStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectDirectoryStatusEntityStored.getId()).thenReturn(2);
        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);
        when(objectDirectoryStatusRepository.getReferenceById(9)).thenReturn(objectDirectoryStatusEntityAwaiting);
        when(transcriptionConfigurationProperties.getAllowedExtensions()).thenReturn(Arrays.asList(DOC, DOCX));
        when(transcriptionConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectDirectoryStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);
        setExpectationsForFailedStates();

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectDirectoryStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }

    @Test
    void processInboundToUnstructuredAnnotation() {

        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);
        String calculatedChecksum = new String(encodeBase64(md5(binaryData.toBytes())));

        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);
        when(externalObjectDirectoryEntityInbound.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(annotationDocumentEntity.getFileName()).thenReturn(TEST_DOC);
        when(annotationDocumentEntity.getFileSize()).thenReturn(binaryData.toString().length());
        when(annotationDocumentEntity.getChecksum()).thenReturn(calculatedChecksum);
        when(objectDirectoryStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectDirectoryStatusEntityStored.getId()).thenReturn(2);
        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);
        when(objectDirectoryStatusRepository.getReferenceById(9)).thenReturn(objectDirectoryStatusEntityAwaiting);
        when(transcriptionConfigurationProperties.getAllowedExtensions()).thenReturn(Arrays.asList(DOC, DOCX));
        when(transcriptionConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectDirectoryStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);
        setExpectationsForFailedStates();

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectDirectoryStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }

    @Test
    void processInboundToUnstructuredFailedFileType() {

        BinaryData binaryData = BinaryData.fromString("test binary data");
        String calculatedChecksum = new String(encodeBase64(md5(binaryData.toBytes())));

        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(mediaEntity.getMediaFile()).thenReturn("test.error");
        when(mediaEntity.getMediaFormat()).thenReturn("mp2");
        when(mediaEntity.getFileSize()).thenReturn((long) binaryData.toString().length());
        when(mediaEntity.getChecksum()).thenReturn(calculatedChecksum);

        when(objectDirectoryStatusEntityFailureFileType.getId()).thenReturn(6);
        when(objectDirectoryStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);
        when(objectDirectoryStatusRepository.getReferenceById(6)).thenReturn(objectDirectoryStatusEntityFailureFileType);
        when(objectDirectoryStatusRepository.getReferenceById(9)).thenReturn(objectDirectoryStatusEntityAwaiting);


        when(audioConfigurationProperties.getAllowedExtensions()).thenReturn(Arrays.asList("doc", "docx"));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(100);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectDirectoryStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);
        setExpectationsForFailedStates();

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectDirectoryStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(FAILURE_FILE_TYPE_CHECK_FAILED.getId(), savedStatus.getId());
        assertEquals(1, externalObjectDirectoryEntityActual.getTransferAttempts());
    }

    @Test
    void processInboundToUnstructuredFailedChecksum() {

        BinaryData binaryData = BinaryData.fromString("test binary data");

        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(mediaEntity.getMediaFile()).thenReturn(TEST_DOC);
        when(mediaEntity.getMediaFormat()).thenReturn("mp2");
        when(mediaEntity.getFileSize()).thenReturn((long) binaryData.toString().length());
        when(mediaEntity.getChecksum()).thenReturn("invalid-checksum");
        when(objectDirectoryStatusEntityFailureChecksum.getId()).thenReturn(7);
        when(objectDirectoryStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);
        when(objectDirectoryStatusRepository.getReferenceById(7)).thenReturn(objectDirectoryStatusEntityFailureChecksum);
        when(objectDirectoryStatusRepository.getReferenceById(9)).thenReturn(objectDirectoryStatusEntityAwaiting);
        when(audioConfigurationProperties.getAllowedExtensions()).thenReturn(Arrays.asList("doc", "docx"));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectDirectoryStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);
        setExpectationsForFailedStates();

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectDirectoryStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(FAILURE_CHECKSUM_FAILED.getId(), savedStatus.getId());
        assertEquals(1, externalObjectDirectoryEntityActual.getTransferAttempts());
    }

    @Test
    void processInboundToUnstructuredFailedFileSize() {

        BinaryData binaryData = BinaryData.fromString("test binary data");
        String calculatedChecksum = new String(encodeBase64(md5(binaryData.toBytes())));

        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(mediaEntity.getMediaFile()).thenReturn("test.doc");
        when(mediaEntity.getFileSize()).thenReturn((long) binaryData.toString().length());
        when(mediaEntity.getChecksum()).thenReturn(calculatedChecksum);
        when(objectDirectoryStatusEntityFailureFileSize.getId()).thenReturn(5);
        when(objectDirectoryStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);
        when(objectDirectoryStatusRepository.getReferenceById(5)).thenReturn(objectDirectoryStatusEntityFailureFileSize);
        when(objectDirectoryStatusRepository.getReferenceById(9)).thenReturn(objectDirectoryStatusEntityAwaiting);
        when(audioConfigurationProperties.getAllowedExtensions()).thenReturn(Arrays.asList("mp2"));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(1);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectDirectoryStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);
        setExpectationsForFailedStates();

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectDirectoryStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(FAILURE_FILE_SIZE_CHECK_FAILED.getId(), savedStatus.getId());
        assertEquals(1, externalObjectDirectoryEntityActual.getTransferAttempts());
    }

    @Test
    void processInboundToUnstructuredOld() {

        BinaryData binaryData = BinaryData.fromString("test binary data");
        String calculatedChecksum = new String(encodeBase64(md5(binaryData.toBytes())));

        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);

        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(mediaEntity.getMediaFile()).thenReturn("test.doc");
        when(mediaEntity.getFileSize()).thenReturn((long) binaryData.toString().length());
        when(mediaEntity.getChecksum()).thenReturn(calculatedChecksum);

        when(objectDirectoryStatusEntityStored.getId()).thenReturn(2);


        when(objectDirectoryStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectDirectoryStatusEntityStored.getId()).thenReturn(2);

        when(objectDirectoryStatusRepository.getReferenceById(2)).thenReturn(objectDirectoryStatusEntityStored);
        when(objectDirectoryStatusRepository.getReferenceById(9)).thenReturn(objectDirectoryStatusEntityAwaiting);

        when(audioConfigurationProperties.getAllowedExtensions()).thenReturn(Arrays.asList("mp2"));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(100);

        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        setExpectationsForFailedStates();

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectDirectoryStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity saved = externalObjectDirectoryEntityCaptor.getValue();

        ObjectDirectoryStatusEntity savedStatus = saved.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());

    }


}
