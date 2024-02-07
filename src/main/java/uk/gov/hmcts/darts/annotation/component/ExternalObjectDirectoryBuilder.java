package uk.gov.hmcts.darts.annotation.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.util.UUID;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Component
@RequiredArgsConstructor
public class ExternalObjectDirectoryBuilder {

    public static final int INITIAL_VERIFICATION_ATTEMPTS = 1;

    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;

    public ExternalObjectDirectoryEntity buildFrom(AnnotationDocumentEntity annotationDocumentEntity, UUID externalLocation) {
        var externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setAnnotationDocumentEntity(annotationDocumentEntity);
        externalObjectDirectoryEntity.setStatus(objectRecordStatusRepository.getReferenceById(STORED.getId()));
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeRepository.getReferenceById(INBOUND.getId()));
        externalObjectDirectoryEntity.setExternalLocation(externalLocation);
        externalObjectDirectoryEntity.setChecksum(annotationDocumentEntity.getChecksum());
        externalObjectDirectoryEntity.setCreatedBy(annotationDocumentEntity.getUploadedBy());
        externalObjectDirectoryEntity.setLastModifiedBy(annotationDocumentEntity.getUploadedBy());
        externalObjectDirectoryEntity.setVerificationAttempts(INITIAL_VERIFICATION_ATTEMPTS);
        return externalObjectDirectoryEntity;
    }
}
