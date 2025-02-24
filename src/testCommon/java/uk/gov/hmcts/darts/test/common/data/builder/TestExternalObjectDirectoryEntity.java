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

@RequiredArgsConstructor
public class TestExternalObjectDirectoryEntity extends ExternalObjectDirectoryEntity implements DbInsertable<ExternalObjectDirectoryEntity> {
    @lombok.Builder
    public TestExternalObjectDirectoryEntity(Integer id, MediaEntity media, TranscriptionDocumentEntity transcriptionDocumentEntity,
                                             AnnotationDocumentEntity annotationDocumentEntity, CaseDocumentEntity caseDocument,
                                             ObjectRecordStatusEntity status,
                                             ExternalLocationTypeEntity externalLocationType, String externalLocation,
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

    @Override
    public ExternalObjectDirectoryEntity getEntity() {
        try {
            ExternalObjectDirectoryEntity annotationEntity = new ExternalObjectDirectoryEntity();
            BeanUtils.copyProperties(annotationEntity, this);
            return annotationEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestExternalObjectDirectoryuilderRetrieve
        implements BuilderHolder<TestExternalObjectDirectoryEntity,
        TestExternalObjectDirectoryEntityBuilder> {
        public TestExternalObjectDirectoryuilderRetrieve() {
        }

        private TestExternalObjectDirectoryEntity.TestExternalObjectDirectoryEntityBuilder builder = TestExternalObjectDirectoryEntity.builder();

        @Override
        public TestExternalObjectDirectoryEntity build() {
            return builder.build();
        }

        @Override
        public TestExternalObjectDirectoryEntity.TestExternalObjectDirectoryEntityBuilder getBuilder() {
            return builder;
        }
    }

}