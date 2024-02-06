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
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_CHECKSUM_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_NOT_FOUND;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_SIZE_CHECK_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_TYPE_CHECK_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class InboundToUnstructuredProcessorImplTest {

    private static final int MAX_FILE_SIZE_VALID = 100;
    private static final String MP2 = "mp2";
    private static final String TEST_DOC = "test.doc";
    private static final String DOC = "doc";
    private static final String DOCX = "docx";
    private static final String TEST_BINARY_DATA = "test binary data";
    @Mock
    ExternalObjectDirectoryEntity externalObjectDirectoryEntityInbound;
    @Mock
    ExternalObjectDirectoryEntity externalObjectDirectoryEntityFailed;
    @Mock
    ExternalLocationTypeEntity externalLocationTypeInbound;
    @Mock
    ExternalLocationTypeEntity externalLocationTypeUnstructured;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityAwaiting;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityStored;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityFailure;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityFailureFileSize;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityFailureFileType;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityFailureChecksum;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityFailureFileNotFound;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityFailureArm;
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

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        inboundToUnstructuredProcessor = new InboundToUnstructuredProcessorImpl(externalObjectDirectoryRepository,
              objectRecordStatusRepository, externalLocationTypeRepository,
              dataManagementService, dataManagementConfiguration, userAccountRepository,
              transcriptionConfigurationProperties, audioConfigurationProperties
        );

    }

    @Test
    void processInboundToUnstructuredMedia() {

        BinaryData binaryData = BinaryData.fromString(TEST_BINARY_DATA);
        String calculatedChecksum = new String(encodeBase64(md5(binaryData.toBytes())));

        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);

        when(mediaEntity.getMediaFormat()).thenReturn(MP2);
        when(mediaEntity.getFileSize()).thenReturn((long) binaryData.toString().length());
        when(mediaEntity.getChecksum()).thenReturn(calculatedChecksum);
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);

        when(audioConfigurationProperties.getAllowedMediaFormats()).thenReturn(List.of(MP2));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectRecordStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }


    @Test
    void processInboundToUnstructuredMediaBlobException() {
        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(objectRecordStatusEntityFailureFileNotFound.getId()).thenReturn(4);
        when(objectRecordStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);
        when(objectRecordStatusRepository.getReferenceById(4)).thenReturn(objectRecordStatusEntityFailureFileNotFound);
        when(dataManagementService.getBlobData(any(), any())).thenThrow(new BlobStorageException("Blobbed it", null, null));

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectRecordStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

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
              .thenReturn(objectRecordStatusEntityFailureChecksum)
              .thenReturn(objectRecordStatusEntityFailureChecksum)
              .thenReturn(objectRecordStatusEntityAwaiting)
              .thenReturn(objectRecordStatusEntityAwaiting)
              .thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusEntityFailureChecksum.getId()).thenReturn(7);

        when(mediaEntity.getMediaFormat()).thenReturn(MP2);
        when(mediaEntity.getFileSize()).thenReturn((long) binaryData.toString().length());
        when(mediaEntity.getChecksum()).thenReturn(calculatedChecksum);
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(audioConfigurationProperties.getAllowedMediaFormats()).thenReturn(List.of(MP2));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectRecordStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);

        List<ExternalObjectDirectoryEntity> failedList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityFailed));
        when(externalObjectDirectoryRepository.findByStatusIdInAndType(anyList(), any())).thenReturn(failedList);

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityActualList = externalObjectDirectoryEntityCaptor.getAllValues();
        ObjectRecordStatusEntity savedStatusUnstructured = externalObjectDirectoryEntityActualList.get(1).getStatus();

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
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);
        when(transcriptionConfigurationProperties.getAllowedExtensions()).thenReturn(Arrays.asList(DOC, DOCX));
        when(transcriptionConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectRecordStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

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
        when(objectRecordStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);
        when(transcriptionConfigurationProperties.getAllowedExtensions()).thenReturn(Arrays.asList(DOC, DOCX));
        when(transcriptionConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectRecordStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }

    @Test
    void processInboundToUnstructuredFailedFileType() {

        BinaryData binaryData = BinaryData.fromString("test binary data");
        String calculatedChecksum = new String(encodeBase64(md5(binaryData.toBytes())));

        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(mediaEntity.getMediaFormat()).thenReturn("mpeg2");
        when(mediaEntity.getFileSize()).thenReturn((long) binaryData.toString().length());
        when(mediaEntity.getChecksum()).thenReturn(calculatedChecksum);

        when(objectRecordStatusEntityFailureFileType.getId()).thenReturn(6);
        when(objectRecordStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(6)).thenReturn(objectRecordStatusEntityFailureFileType);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);

        when(audioConfigurationProperties.getAllowedMediaFormats()).thenReturn(List.of("wave"));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(100);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectRecordStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(FAILURE_FILE_TYPE_CHECK_FAILED.getId(), savedStatus.getId());
        assertEquals(1, externalObjectDirectoryEntityActual.getTransferAttempts());
    }

    @Test
    void processInboundToUnstructuredFailedChecksum() {

        BinaryData binaryData = BinaryData.fromString("test binary data");

        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(mediaEntity.getMediaFormat()).thenReturn(MP2);
        when(mediaEntity.getFileSize()).thenReturn((long) binaryData.toString().length());
        when(mediaEntity.getChecksum()).thenReturn("invalid-checksum");
        when(objectRecordStatusEntityFailureChecksum.getId()).thenReturn(7);
        when(objectRecordStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(7)).thenReturn(objectRecordStatusEntityFailureChecksum);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);
        when(audioConfigurationProperties.getAllowedMediaFormats()).thenReturn(List.of(MP2));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectRecordStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(FAILURE_CHECKSUM_FAILED.getId(), savedStatus.getId());
        assertEquals(1, externalObjectDirectoryEntityActual.getTransferAttempts());
    }

    @Test
    void processInboundToUnstructuredFailedFileSize() {

        BinaryData binaryData = BinaryData.fromString("test binary data");
        String calculatedChecksum = new String(encodeBase64(md5(binaryData.toBytes())));

        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(mediaEntity.getMediaFormat()).thenReturn(MP2);
        when(mediaEntity.getFileSize()).thenReturn((long) binaryData.toString().length());
        when(mediaEntity.getChecksum()).thenReturn(calculatedChecksum);
        when(objectRecordStatusEntityFailureFileSize.getId()).thenReturn(5);
        when(objectRecordStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(5)).thenReturn(objectRecordStatusEntityFailureFileSize);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);
        when(audioConfigurationProperties.getAllowedMediaFormats()).thenReturn(List.of(MP2));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(1);
        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);
        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectRecordStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(FAILURE_FILE_SIZE_CHECK_FAILED.getId(), savedStatus.getId());
        assertEquals(1, externalObjectDirectoryEntityActual.getTransferAttempts());
    }

    @Test
    void processInboundToUnstructuredOld() {

        BinaryData binaryData = BinaryData.fromString("test binary data");
        String calculatedChecksum = new String(encodeBase64(md5(binaryData.toBytes())));

        when(externalLocationTypeRepository.getReferenceById(1)).thenReturn(externalLocationTypeInbound);

        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(mediaEntity.getMediaFormat()).thenReturn(MP2);
        when(mediaEntity.getFileSize()).thenReturn((long) binaryData.toString().length());
        when(mediaEntity.getChecksum()).thenReturn(calculatedChecksum);

        when(objectRecordStatusEntityStored.getId()).thenReturn(2);

        when(objectRecordStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);

        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);

        when(audioConfigurationProperties.getAllowedMediaFormats()).thenReturn(List.of(MP2));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(100);

        when(dataManagementService.getBlobData(any(), any())).thenReturn(binaryData);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryEntityInbound));
        when(externalObjectDirectoryRepository.findByStatusAndType(objectRecordStatusEntityStored, externalLocationTypeInbound)).thenReturn(inboundList);

        inboundToUnstructuredProcessor.processInboundToUnstructured();

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity saved = externalObjectDirectoryEntityCaptor.getValue();

        ObjectRecordStatusEntity savedStatus = saved.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());

    }


}
