package uk.gov.hmcts.darts.annotation.persistence;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnotationPersistenceService {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final HearingRepository hearingRepository;
    private final AnnotationRepository annotationRepository;
    private final AuthorisationApi authorisationApi;

    @Transactional
    public ExternalObjectDirectoryEntity persistAnnotation(ExternalObjectDirectoryEntity externalObjectDirectoryEntity, Integer hearingId) {
        hearingRepository.findById(hearingId)
            .ifPresent(hearingEntity -> hearingEntity.addAnnotation(externalObjectDirectoryEntity.getAnnotationDocumentEntity().getAnnotation()));

        return externalObjectDirectoryRepository.save(externalObjectDirectoryEntity);
    }

    @Transactional
    public void markForDeletion(Integer annotationId) {
        annotationRepository.markForDeletion(annotationId, authorisationApi.getCurrentUser());
    }
}
