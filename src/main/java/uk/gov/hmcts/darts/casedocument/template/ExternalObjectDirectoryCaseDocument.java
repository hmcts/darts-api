package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class ExternalObjectDirectoryCaseDocument extends CreatedModifiedCaseDocument {

    private Integer id;
    private Integer media;
    private Integer transcriptionDocumentEntity;
    private Integer annotationDocumentEntity;
    private Integer caseDocument;
    private ObjectRecordStatusEntity status;
    private ExternalLocationTypeEntity externalLocationType;
    private UUID externalLocation;
    private String externalFileId;
    private String externalRecordId;
    private String checksum;
    private Integer transferAttempts;
    private Integer verificationAttempts;
    private String manifestFile;
    private OffsetDateTime eventDateTs;
    private String errorCode;
    private boolean responseCleaned;
    private boolean updateRetention;
}
