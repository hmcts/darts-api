package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class AnnotationCaseDocument extends CreatedModifiedCaseDocument {

    private final Integer id;
    private String text;
    private OffsetDateTime timestamp;
    private String legacyObjectId;
    private String legacyVersionLabel;
    private Integer currentOwner;
    private boolean deleted;
    private Integer deletedBy;
    private OffsetDateTime deletedTimestamp;
    private List<AnnotationDocumentCaseDocument> annotationDocuments;


    @Data
    public static class AnnotationDocumentCaseDocument {

        private final Integer id;
        private final OffsetDateTime lastModifiedTimestamp;
        private final Integer lastModifiedBy;
        private String fileName;
        private String fileType;
        private Integer fileSize;
        private Integer uploadedBy;
        private OffsetDateTime uploadedDateTime;
        private String checksum;
        private String contentObjectId;
        private String clipId;
        private boolean hidden;
        private OffsetDateTime retainUntilTs;
        private final List<ExternalObjectDirectoryCaseDocument> externalObjectDirectories;
    }
}
