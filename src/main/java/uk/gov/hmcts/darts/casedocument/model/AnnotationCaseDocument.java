package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class AnnotationCaseDocument extends CreatedModifiedCaseDocument {

    private final Integer id;
    private final String text;
    private final OffsetDateTime timestamp;
    private final String legacyObjectId;
    private final String legacyVersionLabel;
    private final Integer currentOwner;
    private final boolean deleted;
    private final Integer deletedBy;
    private final OffsetDateTime deletedTimestamp;
    private final List<AnnotationDocumentCaseDocument> annotationDocuments;


    @Data
    public static class AnnotationDocumentCaseDocument {

        private final Long id;
        private final OffsetDateTime lastModifiedTimestamp;
        private final Integer lastModifiedBy;
        private final String fileName;
        private final String fileType;
        private final Integer fileSize;
        private final Integer uploadedBy;
        private final OffsetDateTime uploadedDateTime;
        private final String checksum;
        private final String contentObjectId;
        private final String clipId;
        private final boolean hidden;
        private final OffsetDateTime retainUntilTs;
        private final List<ExternalObjectDirectoryCaseDocument> externalObjectDirectories;
    }
}
