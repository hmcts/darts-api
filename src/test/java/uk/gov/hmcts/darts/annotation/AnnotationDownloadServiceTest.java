package uk.gov.hmcts.darts.annotation;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.annotation.component.AnnotationDocumentBuilder;
import uk.gov.hmcts.darts.annotation.component.AnnotationMapper;
import uk.gov.hmcts.darts.annotation.component.ExternalObjectDirectoryBuilder;
import uk.gov.hmcts.darts.annotation.persistence.AnnotationPersistenceService;
import uk.gov.hmcts.darts.annotation.service.AnnotationService;
import uk.gov.hmcts.darts.annotation.service.impl.AnnotationServiceImpl;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.datamanagement.api.DataManagementFacade;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadableExternalObjectDirectories;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID;

@ExtendWith(MockitoExtension.class)
class AnnotationDownloadServiceTest {

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
    private ExternalObjectDirectoryRepository eodRepository;
    @Mock
    private DownloadableExternalObjectDirectories downloadableExternalObjectDirectories;

    private AnnotationService annotationService;

    @Mock
    private Validator<Annotation> annotationUploadValidator;
    @Mock
    private Validator<Integer> userAuthorisedToDeleteAnnotationValidator;
    @Mock
    private Validator<Integer> userAuthorisedToDownloadAnnotationValidator;
    @Mock
    private Validator<Integer> annotationExistsValidator;
    @Mock
    private DataManagementFacade dataManagementFacade;
    @Mock
    private DownloadResponseMetaData downloadResponseMetaData;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;


    @BeforeEach
    void setUp() {
        annotationService = new AnnotationServiceImpl(
            annotationMapper,
            annotationDocumentBuilder,
            externalObjectDirectoryBuilder,
            dataManagementApi,
            fileContentChecksum,
            annotationPersistenceService,
            eodRepository,
            annotationUploadValidator,
            userAuthorisedToDeleteAnnotationValidator,
            userAuthorisedToDownloadAnnotationValidator,
            annotationExistsValidator,
            objectRecordStatusRepository,
            dataManagementFacade
        );

    }

    @Test
    void throwsIfDownloadAnnotationDocumentResponseFails() {
        doNothing().when(userAuthorisedToDownloadAnnotationValidator).validate(any());
        when(eodRepository.findByAnnotationIdAndAnnotationDocumentId(any(), any(), any())).thenReturn(
            List.of(someExternalObjectDirectoryEntity()));

        assertThatThrownBy(() -> annotationService.downloadAnnotationDoc(1, 1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT);
    }

    @SneakyThrows
    @Test
    void throwsIfDownloadAnnotationDocumentInputStreamFails() {
        doNothing().when(userAuthorisedToDownloadAnnotationValidator).validate(any());
        when(eodRepository.findByAnnotationIdAndAnnotationDocumentId(any(), any(), any())).thenReturn(
            List.of(someExternalObjectDirectoryEntity()));

        try (MockedStatic<DownloadableExternalObjectDirectories> mockedStatic = Mockito.mockStatic(DownloadableExternalObjectDirectories.class)) {
            when(DownloadableExternalObjectDirectories.getFileBasedDownload(anyList())).thenReturn(downloadableExternalObjectDirectories);
            when(downloadableExternalObjectDirectories.getResponse()).thenReturn(downloadResponseMetaData);
            when(downloadResponseMetaData.isSuccessfulDownload()).thenReturn(true);
            when(downloadResponseMetaData.getInputStream()).thenThrow(new IOException());

            assertThatThrownBy(() -> annotationService.downloadAnnotationDoc(1, 1))
                .isInstanceOf(DartsApiException.class)
                .hasFieldOrPropertyWithValue("error", FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT);

        }
    }

    @Test
    void throwsIfJudgeAndNoAnnotationDocumentFound() {
        doNothing().when(userAuthorisedToDownloadAnnotationValidator).validate(any());

        assertThatThrownBy(() -> annotationService.downloadAnnotationDoc(1, 1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID);
    }

    @Test
    void throwsIfNotJudgeAndNoAnnotationDocumentFound() {

        assertThatThrownBy(() -> annotationService.downloadAnnotationDoc(1, 1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID);
    }

    private AnnotationDocumentEntity someAnnotationDocument() {
        AnnotationDocumentEntity annotationDocumentEntity = new AnnotationDocumentEntity();
        annotationDocumentEntity.setId(1);
        return annotationDocumentEntity;
    }

    private ExternalObjectDirectoryEntity someExternalObjectDirectoryEntity() {
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setId(1);
        externalObjectDirectoryEntity.setExternalLocation(SOME_EXTERNAL_LOCATION);
        externalObjectDirectoryEntity.setAnnotationDocumentEntity(someAnnotationDocument());
        return externalObjectDirectoryEntity;
    }


}
