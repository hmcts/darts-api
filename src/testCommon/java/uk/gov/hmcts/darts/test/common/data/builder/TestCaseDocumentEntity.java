package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestCaseDocumentEntity extends CaseDocumentEntity implements DbInsertable<CaseDocumentEntity> {

    @lombok.Builder
    public TestCaseDocumentEntity(
        Integer id,
        CourtCaseEntity courtCase,
        String fileName,
        String fileType,
        Integer fileSize,
        boolean isDeleted,
        UserAccountEntity deletedBy,
        OffsetDateTime deletedTs,
        String checksum,
        boolean hidden,
        OffsetDateTime retainUntilTs,
        RetentionConfidenceScoreEnum retConfScore,
        String retConfReason,
        OffsetDateTime createdDateTime,
        Integer createdById,
        OffsetDateTime lastModifiedDateTime,
        Integer lastModifiedById
    ) {
        super();
        // Set parent properties
        setId(id);
        setCourtCase(courtCase);
        setFileName(fileName);
        setFileType(fileType);
        setFileSize(fileSize);
        setDeleted(isDeleted);
        setDeletedBy(deletedBy);
        setDeletedTs(deletedTs);
        setChecksum(checksum);
        setHidden(hidden);
        setRetainUntilTs(retainUntilTs);
        setRetConfScore(retConfScore);
        setRetConfReason(retConfReason);
        setCreatedDateTime(createdDateTime);
        setCreatedById(createdById);
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedById(lastModifiedById);
    }

    @Override
    public CaseDocumentEntity getEntity() {
        try {
            CaseDocumentEntity entity = new CaseDocumentEntity();
            BeanUtils.copyProperties(entity, this);
            return entity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestCaseDocumentEntityBuilderRetrieve
        implements BuilderHolder<TestCaseDocumentEntity, TestCaseDocumentEntity.TestCaseDocumentEntityBuilder> {

        private final TestCaseDocumentEntity.TestCaseDocumentEntityBuilder builder = TestCaseDocumentEntity.builder();

        @Override
        public TestCaseDocumentEntity build() {
            return builder.build();
        }

        @Override
        public TestCaseDocumentEntity.TestCaseDocumentEntityBuilder getBuilder() {
            return builder;
        }
    }
}