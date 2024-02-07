package uk.gov.hmcts.darts.annotation.persistence;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnotationPersistenceService {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final HearingRepository hearingRepository;

    @Transactional
    public ExternalObjectDirectoryEntity persistAnnotation(ExternalObjectDirectoryEntity externalObjectDirectoryEntity, Integer hearingId) {
        hearingRepository.findById(hearingId)
            .ifPresent(hearingEntity -> hearingEntity.addAnnotation(externalObjectDirectoryEntity.getAnnotationDocumentEntity().getAnnotation()));

        return externalObjectDirectoryRepository.save(externalObjectDirectoryEntity);
    }
}
