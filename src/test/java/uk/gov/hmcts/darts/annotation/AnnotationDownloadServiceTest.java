package uk.gov.hmcts.darts.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.annotation.component.AnnotationDocumentBuilder;
import uk.gov.hmcts.darts.annotation.component.AnnotationMapper;
import uk.gov.hmcts.darts.annotation.component.ExternalObjectDirectoryBuilder;
import uk.gov.hmcts.darts.annotation.persistence.AnnotationPersistenceService;
import uk.gov.hmcts.darts.annotation.service.AnnotationService;
import uk.gov.hmcts.darts.annotation.service.impl.AnnotationServiceImpl;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID_FOR_JUDGE;

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
    private Validator<Annotation> annotationValidator;

    private AnnotationService annotationService;

    @Mock
    private AuthorisationApi authorisationApi;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;


    @BeforeEach
    void setUp() {
        annotationService = new AnnotationServiceImpl(
                annotationMapper,
                annotationDocumentBuilder,
                externalObjectDirectoryBuilder,
                dataManagementApi,
                fileContentChecksum,
                annotationPersistenceService,
                annotationValidator,
                externalObjectDirectoryRepository,
                authorisationApi
        );

    }

    @Test
    void throwsIfDownloadAnnotationDocumentFails() {
        when(externalObjectDirectoryRepository.findAnnotationIdAndAnnotationDocumentId(any(), any())).thenReturn(
                Optional.of(someExternalObjectDirectoryEntity()));
        when(dataManagementApi.getBlobDataFromInboundContainer(any())).thenThrow(new RuntimeException());

        assertThatThrownBy(() -> annotationService.downloadAnnotationDoc(1, 1))
                .isInstanceOf(DartsApiException.class)
                .hasFieldOrPropertyWithValue("error", FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT);
    }

    @Test
    void throwsIfJudgeAndNoAnnotationDocumentFound() {
        when(externalObjectDirectoryRepository.findAnnotationIdAndAnnotationDocumentId(any(), any())).thenReturn(Optional.empty());
        when(authorisationApi.userHasOneOfRoles(List.of(SecurityRoleEnum.JUDGE))).thenReturn(true);

        assertThatThrownBy(() -> annotationService.downloadAnnotationDoc(1, 1))
                .isInstanceOf(DartsApiException.class)
                .hasFieldOrPropertyWithValue("error", INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID_FOR_JUDGE);
    }

    @Test
    void throwsIfNotJudgeAndNoAnnotationDocumentFound() {
        when(externalObjectDirectoryRepository.findAnnotationIdAndAnnotationDocumentId(any(), any())).thenReturn(Optional.empty());
        when(authorisationApi.userHasOneOfRoles(List.of(SecurityRoleEnum.JUDGE))).thenReturn(false);

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
