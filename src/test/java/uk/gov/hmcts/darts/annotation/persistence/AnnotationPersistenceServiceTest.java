package uk.gov.hmcts.darts.annotation.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.IMPORT_ANNOTATION;

@ExtendWith(MockitoExtension.class)
class AnnotationPersistenceServiceTest {

    private static final int HEARING_ID = 456;
    private static final int ANNOTATION_ID = 123;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private AnnotationRepository annotationRepository;
    @Mock
    private AuthorisationApi authorisationApi;
    @Mock
    private AuditApi auditApi;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private AnnotationDocumentRepository annotationDocumentRepository;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private ExternalObjectDirectoryEntity inboundExternalObjectDirectory;
    @Mock
    private ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory;
    @Mock
    private AnnotationEntity annotationEntity;
    @Mock
    private AnnotationDocumentEntity annotationDocumentEntity;
    @Mock
    private HearingEntity hearing;
    @Mock
    private CourtCaseEntity courtCase;
    @Mock
    private UserAccountEntity userAccount;

    @InjectMocks
    private AnnotationPersistenceService annotationPersistenceService;

    @Test
    void persistAnnotation_resetRetentionProcessingForCases_annotationUploadedAndLinkedCasesFound() {
        // given
        List<Integer> caseIds = List.of(789, 790);
        setupAnnotationPersistence();
        when(caseRepository.findCaseIdsLinkedToAnnotation(ANNOTATION_ID)).thenReturn(caseIds);

        // when
        annotationPersistenceService.persistAnnotation(
            inboundExternalObjectDirectory,
            unstructuredExternalObjectDirectory,
            HEARING_ID,
            annotationEntity,
            annotationDocumentEntity
        );

        // then
        verify(auditApi).record(IMPORT_ANNOTATION, userAccount, courtCase);
        verify(caseRepository).findCaseIdsLinkedToAnnotation(ANNOTATION_ID);
        verify(caseRepository).resetRetentionProcessingForCases(caseIds);
    }

    @Test
    void persistAnnotation_doesNotResetRetentionProcessing_annotationUploadedAndNoLinkedCasesFound() {
        // given
        setupAnnotationPersistence();
        when(caseRepository.findCaseIdsLinkedToAnnotation(ANNOTATION_ID)).thenReturn(Collections.emptyList());

        // when
        annotationPersistenceService.persistAnnotation(
            inboundExternalObjectDirectory,
            unstructuredExternalObjectDirectory,
            HEARING_ID,
            annotationEntity,
            annotationDocumentEntity
        );

        // then
        verify(caseRepository).findCaseIdsLinkedToAnnotation(ANNOTATION_ID);
        verify(caseRepository, times(0)).resetRetentionProcessingForCases(any());
    }

    private void setupAnnotationPersistence() {
        when(hearingRepository.getReferenceById(HEARING_ID)).thenReturn(hearing);
        when(hearing.getCourtCase()).thenReturn(courtCase);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        when(annotationEntity.getId()).thenReturn(ANNOTATION_ID);
    }
}
