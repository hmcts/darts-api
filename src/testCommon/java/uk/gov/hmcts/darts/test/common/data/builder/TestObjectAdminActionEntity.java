package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

// TestClassWithoutTestCases suppression: This is not a test class.
// ConstructorCallsOverridableMethod suppression: If this proves to be a demonstrable problem, we can change the object creation approach. For now, it is fine.
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestObjectAdminActionEntity extends ObjectAdminActionEntity implements DbInsertable<ObjectAdminActionEntity> {
    @lombok.Builder
    public TestObjectAdminActionEntity(Integer id,
                                       AnnotationDocumentEntity annotationDocument,
                                       CaseDocumentEntity caseDocument,
                                       MediaEntity media,
                                       TranscriptionDocumentEntity transcriptionDocument,
                                       ObjectHiddenReasonEntity objectHiddenReason,
                                       UserAccountEntity hiddenBy,
                                       OffsetDateTime hiddenDateTime,
                                       boolean markedForManualDeletion,
                                       UserAccountEntity markedForManualDelBy,
                                       OffsetDateTime markedForManualDelDateTime,
                                       String ticketReference,
                                       String comments) {
        super();
        setId(id);
        setAnnotationDocument(annotationDocument);
        setCaseDocument(caseDocument);
        setMedia(media);
        setTranscriptionDocument(transcriptionDocument);
        setObjectHiddenReason(objectHiddenReason);
        setHiddenBy(hiddenBy);
        setHiddenDateTime(hiddenDateTime);
        setMarkedForManualDeletion(markedForManualDeletion);
        setMarkedForManualDelBy(markedForManualDelBy);
        setMarkedForManualDelDateTime(markedForManualDelDateTime);
        setTicketReference(ticketReference);
        setComments(comments);
    }

    @Override
    public ObjectAdminActionEntity getEntity() {
        try {
            ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
            BeanUtils.copyProperties(objectAdminActionEntity, this);
            return objectAdminActionEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestObjectAdminActionEntityBuilderRetrieve
        implements BuilderHolder<TestObjectAdminActionEntity, TestObjectAdminActionEntity.TestObjectAdminActionEntityBuilder> {

        private final TestObjectAdminActionEntity.TestObjectAdminActionEntityBuilder builder = TestObjectAdminActionEntity.builder();

        @Override
        public TestObjectAdminActionEntity build() {
            return builder.build();
        }

        @Override
        public TestObjectAdminActionEntity.TestObjectAdminActionEntityBuilder getBuilder() {
            return builder;
        }
    }

}