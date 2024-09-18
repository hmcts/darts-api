package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CustomCaseDocumentEntity extends CaseDocumentEntity implements DbInsertable<CaseDocumentEntity> {

    @lombok.Builder
    public CustomCaseDocumentEntity(
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
        Integer retConfScore,
        String retConfReason,
        OffsetDateTime createdDateTime,
        UserAccountEntity createdBy,
        OffsetDateTime lastModifiedDateTime,
        UserAccountEntity lastModifiedBy
    ) {
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
        setCreatedBy(createdBy);
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedBy(lastModifiedBy);
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

    public static class CustomCaseDocumentEntityBuilderRetrieve
        implements BuilderHolder<CustomCaseDocumentEntity, CustomCaseDocumentEntity.CustomCaseDocumentEntityBuilder> {

        private CustomCaseDocumentEntity.CustomCaseDocumentEntityBuilder builder = CustomCaseDocumentEntity.builder();

        @Override
        public CustomCaseDocumentEntity build() {
           return builder.build();
        }

        @Override
        public CustomCaseDocumentEntity.CustomCaseDocumentEntityBuilder getBuilder() {
            return builder;
        }
    }
}