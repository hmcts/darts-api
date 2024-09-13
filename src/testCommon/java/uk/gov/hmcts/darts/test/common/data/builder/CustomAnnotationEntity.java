package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class CustomAnnotationEntity extends AnnotationEntity implements DbInsertable<AnnotationEntity> {

    @lombok.Builder
    public CustomAnnotationEntity(Integer id, String text, OffsetDateTime timestamp, String legacyObjectId,
                                  String legacyVersionLabel, UserAccountEntity currentOwner, boolean deleted,
                                  UserAccountEntity deletedBy, OffsetDateTime deletedTimestamp,
                                  List<AnnotationDocumentEntity> annotationDocuments, List<HearingEntity> hearingList,
                                  OffsetDateTime createdTimestamp,
                                  OffsetDateTime lastModifiedDateTime, UserAccountEntity lastModifiedBy, UserAccountEntity createdBy) {
        setId(id);
        setText(text);
        setTimestamp(timestamp);
        setLegacyObjectId(legacyObjectId);
        setLegacyVersionLabel(legacyVersionLabel);
        setCurrentOwner(currentOwner);
        setDeleted(deleted);
        setDeletedBy(deletedBy);
        setDeletedTimestamp(deletedTimestamp);
        setAnnotationDocuments(annotationDocuments);
        setHearingList(hearingList);
        setCreatedDateTime(createdTimestamp);
        setCreatedBy(createdBy);
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedBy(lastModifiedBy);
    }

    @Override
    public AnnotationEntity getDbInsertable() {
        try {
            AnnotationEntity annotationEntity = new AnnotationEntity();
            BeanUtils.copyProperties(annotationEntity, this);
            return annotationEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class CustomAnnotationEntityRetrieve implements BuilderHolder<CustomAnnotationEntity, CustomAnnotationEntity.CustomAnnotationEntityBuilder> {
        public CustomAnnotationEntityRetrieve() {
        }

        private CustomAnnotationEntity.CustomAnnotationEntityBuilder builder = CustomAnnotationEntity.builder();

        @Override
        public CustomAnnotationEntity build() {
            return builder.build();
        }

        @Override
        public CustomAnnotationEntity.CustomAnnotationEntityBuilder getBuilder() {
            return builder;
        }
    }
}