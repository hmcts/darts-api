package uk.gov.hmcts.darts.annotation;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.annotation.component.AnnotationDocumentBuilder;
import uk.gov.hmcts.darts.annotation.component.AnnotationMapper;
import uk.gov.hmcts.darts.annotation.component.ExternalObjectDirectoryBuilder;
import uk.gov.hmcts.darts.annotation.persistence.AnnotationPersistenceService;
import uk.gov.hmcts.darts.annotation.service.AnnotationService;
import uk.gov.hmcts.darts.annotation.service.impl.AnnotationServiceImpl;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT;

@ExtendWith(MockitoExtension.class)
class AnnotationServiceTest {

    public static final UUID SOME_EXTERNAL_LOCATION = UUID.randomUUID();

    @Mock
    private AnnotationMapper annotationMapper;
    @Mock
    private AnnotationDocumentBuilder annotationDocumentBuilder;
    @Mock
    private ExternalObjectDirectoryBuilder externalObjectDirectoryBuilder;
    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private FileContentChecksum fileContentChecksum;
    @Mock
    private AnnotationPersistenceService annotationPersistenceService;
    @Mock
    private Validator<Annotation> annotationValidator;
    @Mock
    private HearingEntity hearing;

    private final AnnotationEntity annotationEntity = someAnnotationEntity();
    private final AnnotationDocumentEntity annotationDocumentEntity = someAnnotationDocument();
    private final ExternalObjectDirectoryEntity externalObjectDirectoryEntity = someExternalObjectDirectoryEntity();
    private AnnotationService annotationService;


    @BeforeEach
    void setUp() {
        annotationService = new AnnotationServiceImpl(
            annotationMapper,
            annotationDocumentBuilder,
            externalObjectDirectoryBuilder,
            dataManagementApi,
            fileContentChecksum,
            annotationPersistenceService,
            annotationValidator
        );

        when(hearing.getId()).thenReturn(1);
        doNothing().when(annotationValidator).validate(any());
    }

    @Test
    void throwsIfSavingBlobDataToInboundContainerFails() {
        when(dataManagementApi.saveBlobDataToInboundContainer(any())).thenThrow(new RuntimeException());

        assertThatThrownBy(() -> annotationService.process(someMultipartFile(), someAnnotationFor(hearing)))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT);
    }

    @Test
    void throwsIfDocumentInputStreamFails() {
        assertThatThrownBy(() -> annotationService.process(someMultipartFileWithBadInputStream(), someAnnotationFor(hearing)))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT);
    }

    @Test
    void attemptsToDeleteDocumentIfPersistenceFails() throws AzureDeleteBlobException {
        when(annotationMapper.mapFrom(any())).thenReturn(annotationEntity);
        when(dataManagementApi.saveBlobDataToInboundContainer(any())).thenReturn(SOME_EXTERNAL_LOCATION);
        when(annotationPersistenceService.persistAnnotation(any(), any())).thenThrow(new RuntimeException());

        annotationService.process(someMultipartFile(), someAnnotationFor(hearing));

        verify(dataManagementApi).deleteBlobDataFromInboundContainer(SOME_EXTERNAL_LOCATION);
    }

    @Test
    void makesCallToPersistsEntities() throws AzureDeleteBlobException {
        when(annotationMapper.mapFrom(any())).thenReturn(annotationEntity);
        when(annotationDocumentBuilder.buildFrom(any(), any(), any())).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryBuilder.buildFrom(annotationDocumentEntity, SOME_EXTERNAL_LOCATION)).thenReturn(externalObjectDirectoryEntity);
        when(dataManagementApi.saveBlobDataToInboundContainer(any())).thenReturn(SOME_EXTERNAL_LOCATION);
        when(annotationPersistenceService.persistAnnotation(any(), any())).thenReturn(externalObjectDirectoryEntity);

        annotationService.process(someMultipartFile(), someAnnotationFor(hearing));

        verify(annotationPersistenceService).persistAnnotation(externalObjectDirectoryEntity, hearing.getId());
    }

    @Test
    void throwsIfTheAttemptToDeleteDocumentFails() throws AzureDeleteBlobException {
        when(annotationMapper.mapFrom(any())).thenReturn(annotationEntity);
        when(dataManagementApi.saveBlobDataToInboundContainer(any())).thenReturn(SOME_EXTERNAL_LOCATION);
        when(annotationPersistenceService.persistAnnotation(any(), any())).thenThrow(new RuntimeException());
        doThrow(new AzureDeleteBlobException("error"))
            .when(dataManagementApi).deleteBlobDataFromInboundContainer(SOME_EXTERNAL_LOCATION);

        assertThatThrownBy(() -> annotationService.process(someMultipartFile(), someAnnotationFor(hearing)))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT);
    }

    private AnnotationEntity someAnnotationEntity() {
        AnnotationEntity annotationEntity = new AnnotationEntity();
        annotationEntity.setId(1);
        return annotationEntity;
    }

    private AnnotationDocumentEntity someAnnotationDocument() {
        AnnotationDocumentEntity annotationDocumentEntity = new AnnotationDocumentEntity();
        annotationDocumentEntity.setId(1);
        return annotationDocumentEntity;
    }

    private ExternalObjectDirectoryEntity someExternalObjectDirectoryEntity() {
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setId(1);
        return externalObjectDirectoryEntity;
    }


    private MultipartFile someMultipartFile() {
        return new MockMultipartFile(
            "some-multi-part-file",
            "original-filename",
            "some-content-type",
            "some-content".getBytes()
        );
    }

    @SneakyThrows
    private MultipartFile someMultipartFileWithBadInputStream() {
        var unreadableStream = Mockito.mock(MultipartFile.class);
        when(unreadableStream.getInputStream()).thenThrow(new IOException());
        return unreadableStream;
    }

    private Annotation someAnnotationFor(HearingEntity hearing) {
        var annotation = new Annotation();
        annotation.setHearingId(hearing.getId());
        return annotation;
    }

}
