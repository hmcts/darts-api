package uk.gov.hmcts.darts.annotation.service.impl;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.annotation.builders.AnnotationDocumentBuilder;
import uk.gov.hmcts.darts.annotation.builders.AnnotationMapper;
import uk.gov.hmcts.darts.annotation.builders.ExternalObjectDirectoryBuilder;
import uk.gov.hmcts.darts.annotation.persistence.AnnotationPersistenceService;
import uk.gov.hmcts.darts.annotation.service.AnnotationUploadService;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;

@ExtendWith(MockitoExtension.class)
class AnnotationUploadTest {

    @Mock
    private AnnotationMapper annotationMapper;
    @Mock
    private AnnotationDocumentBuilder annotationDocumentBuilder;
    @Mock
    private ExternalObjectDirectoryBuilder externalObjectDirectoryBuilder;
    @Mock
    private AnnotationDataManagement annotationDataManagement;
    @Mock
    private FileContentChecksum fileContentChecksum;
    @Mock
    private AnnotationPersistenceService annotationPersistenceService;
    @Mock
    private Validator<Annotation> hearingExistsValidator;
    @Mock
    private Validator<MultipartFile> fileTypeValidator;
    @Mock
    private HearingEntity hearing;
    @Mock
    private AuditApi auditApi;

    private final AnnotationEntity annotationEntity = someAnnotationEntity();
    private final AnnotationDocumentEntity annotationDocumentEntity = someAnnotationDocument();
    private final ExternalObjectDirectoryEntity externalObjectDirectoryEntityForInboundContainer = someExternalObjectDirectoryEntity();
    private final ExternalObjectDirectoryEntity externalObjectDirectoryEntityForUnstructuredContainer = someExternalObjectDirectoryEntity();
    private AnnotationUploadService uploadService;


    @BeforeEach
    void setUp() {
        uploadService = new AnnotationUploadServiceImpl(
            annotationMapper,
            annotationDocumentBuilder,
            externalObjectDirectoryBuilder,
            fileContentChecksum,
            annotationPersistenceService,
            hearingExistsValidator,
            fileTypeValidator,
            annotationDataManagement
        );

        when(hearing.getId()).thenReturn(1);

        when(hearing.getId()).thenReturn(1);
        doNothing().when(hearingExistsValidator).validate(any());
    }

    @Test
    void throwsIfUploadingDataToContainersFails() {
        when(annotationDataManagement.upload(any(), any())).thenThrow(new DartsApiException(FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT));

        Annotation annotation = someAnnotationFor(hearing);
        assertThatThrownBy(() -> uploadService.upload(someMultipartFile(), annotation))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT);

        verifyNoInteractions(auditApi);
    }

    @Test
    void throwsIfDocumentInputStreamFails() {
        Annotation annotation = someAnnotationFor(hearing);
        assertThatThrownBy(() -> uploadService.upload(someMultipartFileWithBadInputStream(), annotation))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT);

        verifyNoInteractions(auditApi);
    }

    @Test
    void attemptsToDeleteDocumentIfPersistenceFails() {
        var externalBlobLocations = someExternalBlobLocations();
        when(annotationMapper.mapFrom(any())).thenReturn(annotationEntity);
        when(annotationDataManagement.upload(any(), any())).thenReturn(externalBlobLocations);
        doThrow(new RuntimeException())
            .when(annotationPersistenceService).persistAnnotation(
                any(ExternalObjectDirectoryEntity.class),
                any(ExternalObjectDirectoryEntity.class),
                any(Integer.class),
                any(AnnotationEntity.class),
                any(AnnotationDocumentEntity.class));

        uploadService.upload(someMultipartFile(), someAnnotationFor(hearing));

        verify(annotationDataManagement, times(1)).attemptToDeleteDocuments(externalBlobLocations);

        verifyNoInteractions(auditApi);
    }

    @Test
    void makesCallToPersistsEntities() {
        var externalBlobLocations = someExternalBlobLocations();
        when(annotationMapper.mapFrom(any())).thenReturn(annotationEntity);
        when(annotationDocumentBuilder.buildFrom(any(), any())).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryBuilder.buildFrom(annotationDocumentEntity, externalBlobLocations.get(INBOUND), INBOUND))
            .thenReturn(externalObjectDirectoryEntityForInboundContainer);
        when(externalObjectDirectoryBuilder.buildFrom(annotationDocumentEntity, externalBlobLocations.get(UNSTRUCTURED), UNSTRUCTURED))
            .thenReturn(externalObjectDirectoryEntityForUnstructuredContainer);
        when(annotationDataManagement.upload(any(), any())).thenReturn(externalBlobLocations);

        uploadService.upload(someMultipartFile(), someAnnotationFor(hearing));

        verify(externalObjectDirectoryBuilder).buildFrom(annotationDocumentEntity, externalBlobLocations.get(INBOUND), INBOUND);
        verify(externalObjectDirectoryBuilder).buildFrom(annotationDocumentEntity, externalBlobLocations.get(UNSTRUCTURED), UNSTRUCTURED);
        verify(annotationPersistenceService).persistAnnotation(
            externalObjectDirectoryEntityForInboundContainer,
            externalObjectDirectoryEntityForUnstructuredContainer,
            hearing.getId(),
            annotationEntity,
            annotationDocumentEntity);
    }

    private Map<ExternalLocationTypeEnum, String> someExternalBlobLocations() {
        return Map.of(
            INBOUND, UUID.randomUUID().toString(),
            UNSTRUCTURED, UUID.randomUUID().toString()
        );
    }

    private AnnotationEntity someAnnotationEntity() {
        AnnotationEntity annotationEntity = new AnnotationEntity();
        annotationEntity.setId(1);
        return annotationEntity;
    }

    private AnnotationDocumentEntity someAnnotationDocument() {
        AnnotationDocumentEntity annotationDocumentEntity = new AnnotationDocumentEntity();
        annotationDocumentEntity.setId(1L);
        return annotationDocumentEntity;
    }

    private ExternalObjectDirectoryEntity someExternalObjectDirectoryEntity() {
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setId(1L);
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
