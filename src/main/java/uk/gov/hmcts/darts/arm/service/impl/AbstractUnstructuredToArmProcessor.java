package uk.gov.hmcts.darts.arm.service.impl;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;

@Slf4j
public abstract class AbstractUnstructuredToArmProcessor implements UnstructuredToArmProcessor {

    protected final ObjectRecordStatusRepository objectRecordStatusRepository;
    protected final UserIdentity userIdentity;
    protected final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    protected final ExternalLocationTypeRepository externalLocationTypeRepository;
    protected UserAccountEntity userAccount;

    public AbstractUnstructuredToArmProcessor(ObjectRecordStatusRepository objectRecordStatusRepository,
                                              UserIdentity userIdentity,
                                              ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                              ExternalLocationTypeRepository externalLocationTypeRepository) {
        this.objectRecordStatusRepository = objectRecordStatusRepository;
        this.userIdentity = userIdentity;
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.externalLocationTypeRepository = externalLocationTypeRepository;
    }

    protected ExternalObjectDirectoryEntity createArmExternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectory, ObjectRecordStatusEntity status) {

        ExternalObjectDirectoryEntity armExternalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        armExternalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeRepository.getReferenceById(ARM.getId()));
        armExternalObjectDirectoryEntity.setStatus(status);
        armExternalObjectDirectoryEntity.setExternalLocation(externalObjectDirectory.getExternalLocation());
        armExternalObjectDirectoryEntity.setVerificationAttempts(1);

        if (nonNull(externalObjectDirectory.getMedia())) {
            armExternalObjectDirectoryEntity.setMedia(externalObjectDirectory.getMedia());
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            armExternalObjectDirectoryEntity.setTranscriptionDocumentEntity(externalObjectDirectory.getTranscriptionDocumentEntity());
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            armExternalObjectDirectoryEntity.setAnnotationDocumentEntity(externalObjectDirectory.getAnnotationDocumentEntity());
        } else if (nonNull(externalObjectDirectory.getCaseDocument())) {
            armExternalObjectDirectoryEntity.setCaseDocument(externalObjectDirectory.getCaseDocument());
        }
        OffsetDateTime now = OffsetDateTime.now();
        armExternalObjectDirectoryEntity.setCreatedDateTime(now);
        armExternalObjectDirectoryEntity.setLastModifiedDateTime(now);
        var systemUser = userIdentity.getUserAccount();
        armExternalObjectDirectoryEntity.setCreatedBy(systemUser);
        armExternalObjectDirectoryEntity.setLastModifiedBy(systemUser);
        armExternalObjectDirectoryEntity.setTransferAttempts(1);

        return armExternalObjectDirectoryEntity;
    }

    protected void updateExternalObjectDirectoryStatus(ExternalObjectDirectoryEntity armExternalObjectDirectory, ObjectRecordStatusEntity armStatus) {
        log.debug(
            "Updating ARM status from {} to {} for ID {}",
            armExternalObjectDirectory.getStatus().getDescription(),
            armStatus.getDescription(),
            armExternalObjectDirectory.getId()
        );
        armExternalObjectDirectory.setStatus(armStatus);
        armExternalObjectDirectory.setLastModifiedBy(userAccount);
        externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
    }

    protected Optional<ExternalObjectDirectoryEntity> getUnstructuredExternalObjectDirectoryEntity(
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity, ObjectRecordStatusEntity status) {

        return externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
            status,
            externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.UNSTRUCTURED.getId()),
            externalObjectDirectoryEntity.getMedia(),
            externalObjectDirectoryEntity.getTranscriptionDocumentEntity(),
            externalObjectDirectoryEntity.getAnnotationDocumentEntity(),
            externalObjectDirectoryEntity.getCaseDocument()
        );
    }

    protected void updateTransferAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        int currentNumberOfAttempts = externalObjectDirectoryEntity.getTransferAttempts();
        externalObjectDirectoryEntity.setTransferAttempts(currentNumberOfAttempts + 1);
    }

    public String generateFilename(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        final Integer entityId = externalObjectDirectoryEntity.getId();
        final Integer transferAttempts = externalObjectDirectoryEntity.getTransferAttempts();

        Integer documentId = 0;
        if (nonNull(externalObjectDirectoryEntity.getMedia())) {
            documentId = externalObjectDirectoryEntity.getMedia().getId();
        } else if (nonNull(externalObjectDirectoryEntity.getTranscriptionDocumentEntity())) {
            documentId = externalObjectDirectoryEntity.getTranscriptionDocumentEntity().getId();
        } else if (nonNull(externalObjectDirectoryEntity.getAnnotationDocumentEntity())) {
            documentId = externalObjectDirectoryEntity.getAnnotationDocumentEntity().getId();
        } else if (nonNull(externalObjectDirectoryEntity.getCaseDocument())) {
            documentId = externalObjectDirectoryEntity.getCaseDocument().getId();
        }

        return String.format("%s_%s_%s", entityId, documentId, transferAttempts);
    }
}
