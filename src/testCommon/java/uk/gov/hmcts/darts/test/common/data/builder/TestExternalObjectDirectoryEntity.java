package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.NoArgsConstructor;
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

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
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
                                             Integer createdById, OffsetDateTime lastModifiedDateTime, Integer lastModifiedById) {
        super();
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
        setInputUploadProcessedTs(dataIngestionTs);
        setManifestFile(manifestFile);
        setEventDateTs(eventDateTs);
        setErrorCode(errorCode);
        setResponseCleaned(responseCleaned);
        setOsrUuid(osrUuid);
        setUpdateRetention(updateRetention);
        setCreatedDateTime(createdDateTime);
        setCreatedById(createdById);
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedById(lastModifiedById);
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

    @NoArgsConstructor
    public static class TestExternalObjectDirectoryBuilderRetrieve
        implements BuilderHolder<TestExternalObjectDirectoryEntity,
        TestExternalObjectDirectoryEntityBuilder> {

        private final TestExternalObjectDirectoryEntity.TestExternalObjectDirectoryEntityBuilder builder = TestExternalObjectDirectoryEntity.builder();

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