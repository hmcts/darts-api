package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TranscriptionCaseDocument extends CreatedModifiedCaseDocument {

    private final Long id;
    private final TranscriptionTypeEntity transcriptionType;
    private final TranscriptionUrgencyEntity transcriptionUrgency;
    private final TranscriptionStatusEntity transcriptionStatus;
    private final String legacyObjectId;
    private final String requestor;
    private final LocalDate hearingDate;
    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
    private final String legacyVersionLabel;
    private final Boolean isManualTranscription;
    private final Boolean hideRequestFromRequestor;
    private final Boolean deleted;
    private final Integer deletedBy;
    private final OffsetDateTime deletedTimestamp;
    private final String chronicleId;
    private final String antecedentId;
    private final List<TranscriptionCommentCaseDocument> transcriptionComments;
    private final List<TranscriptionWorkflowCaseDocument> transcriptionWorkflows;
    private final List<TranscriptionDocumentCaseDocument> transcriptionDocuments;

    @Data
    public static class TranscriptionDocumentCaseDocument {

        private final Long id;
        private final OffsetDateTime lastModifiedTimestamp;
        private final Integer lastModifiedBy;
        private final String clipId;
        private final String fileName;
        private final String fileType;
        private final Integer fileSize;
        private final Integer uploadedBy;
        private final OffsetDateTime uploadedDateTime;
        private final String checksum;
        private final String contentObjectId;
        private final boolean hidden;
        private final OffsetDateTime retainUntilTs;
        private final List<ExternalObjectDirectoryCaseDocument> externalObjectDirectories;
        private final List<ObjectAdminActionCaseDocument> adminActions;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class TranscriptionCommentCaseDocument extends CreatedModifiedCaseDocument {

        private final Integer id;
        private final Integer transcriptionWorkflow;
        private String legacyTranscriptionObjectId;
        private String comment;
        private OffsetDateTime commentTimestamp;
        private Integer authorUserId;
    }

    @Data
    public static class TranscriptionWorkflowCaseDocument {

        private final Integer id;
        private TranscriptionStatusEntity transcriptionStatus;
        private Integer workflowActor;
        private OffsetDateTime workflowTimestamp;
        private List<TranscriptionCommentCaseDocument> transcriptionComments;
    }
}
