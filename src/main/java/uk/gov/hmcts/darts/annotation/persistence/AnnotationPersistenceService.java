package uk.gov.hmcts.darts.annotation.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;

import static uk.gov.hmcts.darts.audit.api.AuditActivity.IMPORT_ANNOTATION;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnotationPersistenceService {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final HearingRepository hearingRepository;
    private final AnnotationRepository annotationRepository;
    private final AuthorisationApi authorisationApi;
    private final AuditApi auditApi;
    private final UserIdentity userIdentity;
    private final AnnotationDocumentRepository annotationDocumentRepository;

    @Transactional
    public void persistAnnotation(
            ExternalObjectDirectoryEntity inboundExternalObjectDirectory,
            ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory,
            Integer hearingId,
            AnnotationEntity annotationEntity,
            AnnotationDocumentEntity annotationDocumentEntity) {

        annotationRepository.saveAndFlush(annotationEntity);

        final var hearing = hearingRepository.getReferenceById(hearingId);
        hearing.addAnnotation(annotationEntity);
        hearingRepository.save(hearing);

        annotationDocumentEntity.setAnnotation(annotationEntity);
        annotationDocumentRepository.save(annotationDocumentEntity);

        inboundExternalObjectDirectory.setAnnotationDocumentEntity(annotationDocumentEntity);
        unstructuredExternalObjectDirectory.setAnnotationDocumentEntity(annotationDocumentEntity);
        externalObjectDirectoryRepository.save(inboundExternalObjectDirectory);
        externalObjectDirectoryRepository.save(unstructuredExternalObjectDirectory);

        auditApi.record(IMPORT_ANNOTATION, userIdentity.getUserAccount(), hearing.getCourtCase());
    }

    @Transactional
    public void markForDeletion(Integer annotationId) {
        annotationRepository.markForDeletion(annotationId, authorisationApi.getCurrentUser().getId());
    }
}
