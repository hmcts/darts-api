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
import org.mockito.stubbing.Answer;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.impl.InboundToUnstructuredProcessorSingleElementImpl;
import uk.gov.hmcts.darts.transcriptions.config.TranscriptionConfigurationProperties;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_ARM_INGESTION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_CHECKSUM_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_EMPTY_FILE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_NOT_FOUND;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_SIZE_CHECK_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_TYPE_CHECK_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@ExtendWith(MockitoExtension.class)
class InboundToUnstructuredProcessorSingleElementImplTest {

    private static final int MAX_FILE_SIZE_VALID = 100;
    private static final String MP2 = "mp2";
    private static final String TEST_DOC = "test.doc";
    private static final String DOC = "doc";
    private static final String DOCX = "docx";
    private static final Integer INBOUND_ID = 5555;
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
    private InboundToUnstructuredProcessorSingleElementImpl inboundToUnstructuredProcessor;
    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;
    @Mock
    private MediaEntity mediaEntity;
    @Mock
    private TranscriptionDocumentEntity transcriptionDocumentEntity;
    @Mock
    private AnnotationDocumentEntity annotationDocumentEntity;
    @Mock
    private CaseDocumentEntity caseDocumentEntity;
    @Mock
    ExternalObjectDirectoryEntity externalObjectDirectoryEntityInbound;
    @Mock
    ExternalObjectDirectoryEntity externalObjectDirectoryEntityFailed;
    @Mock
    ExternalLocationTypeEntity externalLocationTypeUnstructured;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityAwaiting;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityStored;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityFailureFileSize;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityFailureFileType;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityFailureChecksum;
    @Mock
    ObjectRecordStatusEntity objectRecordStatusEntityFailureFileNotFound;
    @Mock
    FileContentChecksum fileContentChecksum;

    @Mock
    MediaRepository mediaRepository;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        inboundToUnstructuredProcessor = new InboundToUnstructuredProcessorSingleElementImpl(dataManagementService, dataManagementConfiguration,
                                                                                             userAccountRepository, objectRecordStatusRepository,
                                                                                             externalLocationTypeRepository,
                                                                                             transcriptionConfigurationProperties,
                                                                                             audioConfigurationProperties,
                                                                                             externalObjectDirectoryRepository, mediaRepository,
                                                                                             fileContentChecksum);
        when(externalObjectDirectoryRepository.findById(INBOUND_ID)).thenReturn(Optional.of(externalObjectDirectoryEntityInbound));
    }

    @Test
    void processInboundToUnstructuredMedia() {

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);

        when(mediaEntity.getMediaFormat()).thenReturn(MP2);
        when(mediaEntity.getChecksum()).thenReturn("checksum");
        when(mediaRepository.findById(any())).thenReturn(Optional.of(mediaEntity));
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);

        when(audioConfigurationProperties.getAllowedMediaFormats()).thenReturn(List.of(MP2));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.downloadBlobToFile(any(), any(), any())).thenAnswer(writeBlobToFile());
        when(fileContentChecksum.calculate(any(Path.class))).thenReturn("checksum");

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }

    @Test
    void processInboundToUnstructuredMediaWithUnstructuredFailed() {

        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntityFailed.getStatus())
            .thenReturn(objectRecordStatusEntityAwaiting)
            .thenReturn(objectRecordStatusEntityStored);

        when(mediaEntity.getMediaFormat()).thenReturn(MP2);
        when(mediaEntity.getChecksum()).thenReturn("checksum");
        when(mediaRepository.findById(any())).thenReturn(Optional.of(mediaEntity));

        when(objectRecordStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);
        when(audioConfigurationProperties.getAllowedMediaFormats()).thenReturn(List.of(MP2));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.downloadBlobToFile(any(), any(), any())).thenAnswer(writeBlobToFile());
        when(fileContentChecksum.calculate(any(Path.class))).thenReturn("checksum");

        when(externalObjectDirectoryRepository.findByIdsAndFailure(mediaEntity.getId(), null, null, null,
           List.of(
               FAILURE.getId(),
               FAILURE_FILE_NOT_FOUND.getId(),
               FAILURE_FILE_SIZE_CHECK_FAILED.getId(),
               FAILURE_FILE_TYPE_CHECK_FAILED.getId(),
               FAILURE_CHECKSUM_FAILED.getId(),
               FAILURE_ARM_INGESTION_FAILED.getId(),
               FAILURE_EMPTY_FILE.getId()))).thenReturn(externalObjectDirectoryEntityFailed);

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityActualList = externalObjectDirectoryEntityCaptor.getAllValues();
        ObjectRecordStatusEntity savedStatusUnstructured = externalObjectDirectoryEntityActualList.get(1).getStatus();

        assertEquals(STORED.getId(), savedStatusUnstructured.getId());
    }


    @Test
    void processInboundToUnstructuredTranscription() {

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalObjectDirectoryEntityInbound.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(transcriptionDocumentEntity.getFileName()).thenReturn(TEST_DOC);
        when(transcriptionDocumentEntity.getFileSize()).thenReturn(33);
        when(transcriptionDocumentEntity.getChecksum()).thenReturn("checksum");
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);
        when(transcriptionConfigurationProperties.getAllowedExtensions()).thenReturn(Arrays.asList(DOC, DOCX));
        when(transcriptionConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.downloadBlobToFile(any(), any(), any())).thenAnswer(writeBlobToFile());
        when(fileContentChecksum.calculate(any(Path.class))).thenReturn("checksum");

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }

    @Test
    void processInboundToUnstructuredAnnotation() {

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalObjectDirectoryEntityInbound.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(annotationDocumentEntity.getFileName()).thenReturn(TEST_DOC);
        when(annotationDocumentEntity.getFileSize()).thenReturn(33);
        when(annotationDocumentEntity.getChecksum()).thenReturn("checksum");
        when(objectRecordStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);
        when(transcriptionConfigurationProperties.getAllowedExtensions()).thenReturn(Arrays.asList(DOC, DOCX));
        when(transcriptionConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.downloadBlobToFile(any(), any(), any())).thenAnswer(writeBlobToFile());
        when(fileContentChecksum.calculate(any(Path.class))).thenReturn("checksum");

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }

    @Test
    void processInboundToUnstructuredCaseDocument() {

        when(externalObjectDirectoryEntityInbound.getCaseDocument()).thenReturn(caseDocumentEntity);
        when(caseDocumentEntity.getId()).thenReturn(44);
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);
        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);
        when(dataManagementService.downloadBlobToFile(any(), any(), any())).thenAnswer(writeBlobToFile());
        when(fileContentChecksum.calculate(any(Path.class))).thenReturn("checksum");

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());
    }

    @Test
    void processInboundToUnstructuredFailedChecksum() {

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(mediaEntity.getMediaFormat()).thenReturn(MP2);
        when(mediaEntity.getChecksum()).thenReturn("invalid-checksum");
        when(mediaRepository.findById(any())).thenReturn(Optional.of(mediaEntity));

        when(objectRecordStatusEntityFailureChecksum.getId()).thenReturn(7);
        when(objectRecordStatusRepository.getReferenceById(7)).thenReturn(objectRecordStatusEntityFailureChecksum);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);
        when(audioConfigurationProperties.getAllowedMediaFormats()).thenReturn(Arrays.asList(MP2));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(MAX_FILE_SIZE_VALID);
        when(dataManagementService.downloadBlobToFile(any(), any(), any())).thenAnswer(writeBlobToFile());
        when(fileContentChecksum.calculate(any(Path.class))).thenReturn("checksum");

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(FAILURE_CHECKSUM_FAILED.getId(), savedStatus.getId());
        assertEquals(1, externalObjectDirectoryEntityActual.getTransferAttempts());
    }

    @Test
    void processInboundToUnstructuredFailedFileSize() {

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);
        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(mediaEntity.getMediaFormat()).thenReturn(MP2);
        when(mediaEntity.getChecksum()).thenReturn("checksum");
        when(mediaRepository.findById(any())).thenReturn(Optional.of(mediaEntity));

        when(objectRecordStatusEntityFailureFileSize.getId()).thenReturn(5);
        when(objectRecordStatusRepository.getReferenceById(5)).thenReturn(objectRecordStatusEntityFailureFileSize);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);
        when(audioConfigurationProperties.getAllowedMediaFormats()).thenReturn(Arrays.asList(MP2));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(1);
        when(dataManagementService.downloadBlobToFile(any(), any(), any())).thenAnswer(writeBlobToFile());
        when(fileContentChecksum.calculate(any(Path.class))).thenReturn("checksum");

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(externalObjectDirectoryRepository, times(2)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntityActual = externalObjectDirectoryEntityCaptor.getValue();
        ObjectRecordStatusEntity savedStatus = externalObjectDirectoryEntityActual.getStatus();

        assertEquals(FAILURE_FILE_SIZE_CHECK_FAILED.getId(), savedStatus.getId());
        assertEquals(1, externalObjectDirectoryEntityActual.getTransferAttempts());
    }

    @Test
    void processInboundToUnstructuredOld() {

        when(externalLocationTypeRepository.getReferenceById(2)).thenReturn(externalLocationTypeUnstructured);

        when(externalObjectDirectoryEntityInbound.getMedia()).thenReturn(mediaEntity);
        when(mediaEntity.getMediaFormat()).thenReturn(MP2);
        when(mediaEntity.getChecksum()).thenReturn("checksum");
        when(mediaRepository.findById(any())).thenReturn(Optional.of(mediaEntity));

        when(objectRecordStatusEntityStored.getId()).thenReturn(2);


        when(objectRecordStatusEntityAwaiting.getId()).thenReturn(9);
        when(objectRecordStatusEntityStored.getId()).thenReturn(2);

        when(objectRecordStatusRepository.getReferenceById(2)).thenReturn(objectRecordStatusEntityStored);
        when(objectRecordStatusRepository.getReferenceById(9)).thenReturn(objectRecordStatusEntityAwaiting);

        when(audioConfigurationProperties.getAllowedMediaFormats()).thenReturn(List.of(MP2));
        when(audioConfigurationProperties.getMaxFileSize()).thenReturn(100);

        when(dataManagementService.downloadBlobToFile(any(), any(), any())).thenAnswer(writeBlobToFile());
        when(fileContentChecksum.calculate(any(Path.class))).thenReturn("checksum");

        inboundToUnstructuredProcessor.processSingleElement(INBOUND_ID);

        verify(externalObjectDirectoryRepository, times(3)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        ExternalObjectDirectoryEntity saved = externalObjectDirectoryEntityCaptor.getValue();

        ObjectRecordStatusEntity savedStatus = saved.getStatus();

        assertEquals(STORED.getId(), savedStatus.getId());

    }

    private static Answer<Object> writeBlobToFile() {
        return invocationOnMock -> {
            File downloadedInboundTestBlobDataFile = File.createTempFile("testInboundBlob", ".tmp");
            Files.write(downloadedInboundTestBlobDataFile.toPath(), "someContent".getBytes());
            return downloadedInboundTestBlobDataFile.toPath();
        };
    }

}