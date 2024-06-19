package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CaseDocumentCaseDocument extends CreatedModifiedCaseDocument {

    private final Integer id;
    private final String fileName;
    private final String fileType;
    private final Integer fileSize;
    private final String checksum;
    private final boolean hidden;
    private final OffsetDateTime retainUntilTs;
    private final List<ExternalObjectDirectoryCaseDocument> externalObjectDirectories;
}
