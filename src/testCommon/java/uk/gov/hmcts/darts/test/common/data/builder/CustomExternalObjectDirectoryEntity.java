package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CustomExternalObjectDirectoryEntity extends ExternalObjectDirectoryEntity {
    @lombok.Builder
    public CustomExternalObjectDirectoryEntity(Integer id, MediaEntity media, TranscriptionDocumentEntity transcriptionDocumentEntity,
                                               AnnotationDocumentEntity annotationDocumentEntity, CaseDocumentEntity caseDocument,
                                               ObjectRecordStatusEntity status,
                                               ExternalLocationTypeEntity externalLocationType, UUID externalLocation,
                                               String externalFileId, String externalRecordId,
                                               String checksum, Integer transferAttempts, Integer verificationAttempts,
                                               OffsetDateTime dataIngestionTs, String manifestFile,
                                               OffsetDateTime eventDateTs, String errorCode, boolean responseCleaned, Long osrUuid,
                                               boolean updateRetention, OffsetDateTime createdDateTime,
                                               UserAccountEntity createdBy, OffsetDateTime lastModifiedDateTime, UserAccountEntity lastModifiedBy) {
        setId(id);
        setMedia(media);
        setTranscriptionDocumentEntity(transcriptionDocumentEntity);
        setAnnotationDocumentEntity(annotationDocumentEntity);
        setCaseDocument(caseDocument);
        setStatus(status);
        setExternalLocationType(externalLocationType);
        setExternalLocation(externalLocation);
        setExternalFileId(externalFileId);
        setExternalRecordId(externalRecordId);
        setChecksum(checksum);
        setTransferAttempts(transferAttempts);
        setVerificationAttempts(verificationAttempts);
        setDataIngestionTs(dataIngestionTs);
        setManifestFile(manifestFile);
        setEventDateTs(eventDateTs);
        setErrorCode(errorCode);
        setResponseCleaned(responseCleaned);
        setOsrUuid(osrUuid);
        setUpdateRetention(updateRetention);
        setCreatedDateTime(createdDateTime);
        setCreatedBy(createdBy);
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedBy(lastModifiedBy);
    }

    public static class CustomExternalObjectDirectoryuilderRetrieve
        implements BuilderHolder<ExternalObjectDirectoryEntity,
                CustomExternalObjectDirectoryEntityBuilder> {
        public CustomExternalObjectDirectoryuilderRetrieve() {
        }

        private CustomExternalObjectDirectoryEntity.CustomExternalObjectDirectoryEntityBuilder builder = CustomExternalObjectDirectoryEntity.builder();

        @Override
        public ExternalObjectDirectoryEntity build() {
            try {
                ExternalObjectDirectoryEntity externalObjectDirectory = new ExternalObjectDirectoryEntity();
                BeanUtils.copyProperties(externalObjectDirectory, builder.build());
                return externalObjectDirectory;
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
            }
        }

        @Override
        public CustomExternalObjectDirectoryEntity.CustomExternalObjectDirectoryEntityBuilder getBuilder() {
            return builder;
        }
    }

}