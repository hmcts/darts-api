package uk.gov.hmcts.darts.testutils.stubs;

import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;

import java.time.OffsetDateTime;
import javax.annotation.PostConstruct;

@Component
@Deprecated
public class ObjectAdminActionStub {

    @Autowired
    private UserAccountStub userAccountStub;

    @Autowired
    private ObjectHiddenReasonStub objectHiddenReasonStub;

    @Autowired
    private ObjectAdminActionRepository objectAdminActionRepository;

    private static UserAccountEntity defaultUser;
    private static ObjectHiddenReasonEntity defaultReason;

    private static final OffsetDateTime DEFAULT_DATE_TIME = OffsetDateTime.parse("2024-01-01T00:00:00.000Z");

    @PostConstruct
    public void init() {
        defaultUser = userAccountStub.getSystemUserAccountEntity();
        defaultReason = objectHiddenReasonStub.getAnyWithMarkedForDeletion(true);
    }

    public ObjectAdminActionEntity createAndSave(ObjectAdminActionSpec builder) {
        var objectAdminActionEntity = new ObjectAdminActionEntity();
        objectAdminActionEntity.setAnnotationDocument(builder.annotationDocument);
        objectAdminActionEntity.setCaseDocument(builder.caseDocument);
        objectAdminActionEntity.setMedia(builder.media);
        objectAdminActionEntity.setTranscriptionDocument(builder.transcriptionDocument);
        objectAdminActionEntity.setObjectHiddenReason(builder.objectHiddenReason);
        objectAdminActionEntity.setHiddenBy(builder.hiddenBy);
        objectAdminActionEntity.setHiddenDateTime(builder.hiddenDateTime);
        objectAdminActionEntity.setMarkedForManualDeletion(builder.markedForManualDeletion);
        objectAdminActionEntity.setMarkedForManualDelBy(builder.markedForManualDelBy);
        objectAdminActionEntity.setMarkedForManualDelDateTime(builder.markedForManualDelDateTime);
        objectAdminActionEntity.setTicketReference(builder.ticketReference);
        objectAdminActionEntity.setComments(builder.comments);

        return createAndSave(objectAdminActionEntity);
    }

    public ObjectAdminActionEntity createAndSave(ObjectAdminActionEntity objectAdminActionEntity) {
        return objectAdminActionRepository.save(objectAdminActionEntity);
    }

    @Builder
    public static class ObjectAdminActionSpec {
        private AnnotationDocumentEntity annotationDocument;
        private CaseDocumentEntity caseDocument;
        private MediaEntity media;
        private TranscriptionDocumentEntity transcriptionDocument;
        @Builder.Default
        private ObjectHiddenReasonEntity objectHiddenReason = defaultReason;
        @Builder.Default
        private UserAccountEntity hiddenBy = defaultUser;
        @Builder.Default
        private OffsetDateTime hiddenDateTime = DEFAULT_DATE_TIME;
        @Builder.Default
        private boolean markedForManualDeletion = true;
        @Builder.Default
        private UserAccountEntity markedForManualDelBy = defaultUser;
        @Builder.Default
        private OffsetDateTime markedForManualDelDateTime = DEFAULT_DATE_TIME;
        @Builder.Default
        private String ticketReference = "Some ticket reference";
        @Builder.Default
        private String comments = "Some comment";
    }

}