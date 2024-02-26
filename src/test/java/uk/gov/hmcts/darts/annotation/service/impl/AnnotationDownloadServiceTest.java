package uk.gov.hmcts.darts.annotation.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.annotation.service.AnnotationDownloadService;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID;

@ExtendWith(MockitoExtension.class)
class AnnotationDownloadServiceTest {

    @Mock
    private AnnotationDataManagement annotationDataManagement;
    @Mock
    private ExternalObjectDirectoryRepository eodRepository;
    @Mock
    private Validator<Integer> userAuthorisedToDownloadAnnotationValidator;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;

    private AnnotationDownloadService downloadService;

    @BeforeEach
    void setUp() {
        downloadService = new AnnotationDownloadServiceImpl(
            annotationDataManagement,
            eodRepository,
            userAuthorisedToDownloadAnnotationValidator,
            objectRecordStatusRepository);
    }

    @Test
    void throwsIfJudgeAndNoAnnotationDocumentFound() {
        doNothing().when(userAuthorisedToDownloadAnnotationValidator).validate(any());

        assertThatThrownBy(() -> downloadService.downloadAnnotationDoc(1, 1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID);
    }

    @Test
    void throwsIfNotJudgeAndNoAnnotationDocumentFound() {

        assertThatThrownBy(() -> downloadService.downloadAnnotationDoc(1, 1))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID);
    }
}
