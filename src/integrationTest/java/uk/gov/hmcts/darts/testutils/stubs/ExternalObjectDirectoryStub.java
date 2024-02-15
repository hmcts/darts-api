package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExternalObjectDirectoryStub {

    private final UserAccountStub userAccountStub;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalObjectDirectoryRepository eodRepository;

    /**
     * @deprecated
     * <p> Use {@link ExternalObjectDirectoryStub#createExternalObjectDirectory(MediaEntity, ObjectRecordStatusEnum, ExternalLocationTypeEnum, UUID)} instead.
     */
    @Deprecated
    public ExternalObjectDirectoryEntity createExternalObjectDirectory(MediaEntity mediaEntity,
                                                                       ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                       ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                       UUID externalLocation) {
        ExternalObjectDirectoryEntity externalObjectDirectory = createMinimalExternalObjectDirectory(
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            externalLocation
        );

        externalObjectDirectory.setMedia(mediaEntity);

        return externalObjectDirectory;
    }

    public ExternalObjectDirectoryEntity createAndSaveEOD(MediaEntity media,
                                                          ObjectRecordStatusEnum objectRecordStatusEnum,
                                                          ExternalLocationTypeEnum externalLocationTypeEnum) {
        UUID uuid = UUID.randomUUID();
        var eod = createExternalObjectDirectory(media, objectRecordStatusEnum, externalLocationTypeEnum, uuid);
        return eodRepository.save(eod);
    }

    public ExternalObjectDirectoryEntity createExternalObjectDirectory(MediaEntity media,
                                                                       ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                       ExternalLocationTypeEnum externalLocationTypeEnum,
                                                                       UUID externalLocation) {

        return createExternalObjectDirectory(media, getStatus(objectRecordStatusEnum), getLocation(externalLocationTypeEnum), externalLocation);
    }

    public ExternalObjectDirectoryEntity createExternalObjectDirectory(AnnotationDocumentEntity annotationDocumentEntity,
                                                                       ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                       ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                       UUID externalLocation) {
        ExternalObjectDirectoryEntity externalObjectDirectory = createMinimalExternalObjectDirectory(
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            externalLocation
        );

        externalObjectDirectory.setAnnotationDocumentEntity(annotationDocumentEntity);

        return externalObjectDirectory;
    }


    public ExternalObjectDirectoryEntity createExternalObjectDirectory(TranscriptionDocumentEntity transcriptionDocumentEntity,
                                                                       ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                       ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                       UUID externalLocation) {
        ExternalObjectDirectoryEntity externalObjectDirectory = createMinimalExternalObjectDirectory(
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            externalLocation
        );

        externalObjectDirectory.setTranscriptionDocumentEntity(transcriptionDocumentEntity);

        return externalObjectDirectory;
    }

    public List<ExternalObjectDirectoryEntity> findByMediaStatusAndType(MediaEntity media,
                                                                        ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                        ExternalLocationTypeEnum externalLocationTypeEnum) {

        return eodRepository.findByMediaStatusAndType(media, getStatus(objectRecordStatusEnum), getLocation(externalLocationTypeEnum));
    }

    private ExternalObjectDirectoryEntity createMinimalExternalObjectDirectory(ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                               ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                               UUID externalLocation) {
        var externalObjectDirectory = new ExternalObjectDirectoryEntity();
        externalObjectDirectory.setStatus(objectRecordStatusEntity);
        externalObjectDirectory.setExternalLocationType(externalLocationTypeEntity);
        externalObjectDirectory.setExternalLocation(externalLocation);
        externalObjectDirectory.setChecksum(null);
        externalObjectDirectory.setTransferAttempts(null);
        externalObjectDirectory.setVerificationAttempts(1);

        var user = userAccountStub.getIntegrationTestUserAccountEntity();
        externalObjectDirectory.setCreatedBy(user);
        externalObjectDirectory.setLastModifiedBy(user);
        return externalObjectDirectory;
    }

    private ExternalLocationTypeEntity getLocation(ExternalLocationTypeEnum externalLocationTypeEnum) {
        return externalLocationTypeRepository.getReferenceById(externalLocationTypeEnum.getId());
    }

    private ObjectRecordStatusEntity getStatus(ObjectRecordStatusEnum objectRecordStatusEnum) {
        return objectRecordStatusRepository.getReferenceById(objectRecordStatusEnum.getId());
    }

}
