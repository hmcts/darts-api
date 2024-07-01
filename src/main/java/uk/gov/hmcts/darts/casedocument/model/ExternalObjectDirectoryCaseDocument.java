package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class ExternalObjectDirectoryCaseDocument extends CreatedModifiedCaseDocument {

    private final Integer id;
    private final Integer media;
    private final Integer transcriptionDocument;
    private final Integer annotationDocument;
    private final Integer caseDocument;
    private final ObjectRecordStatusEntity status;
    private final ExternalLocationTypeEntity externalLocationType;
    private final UUID externalLocation;
    private final String externalFileId;
    private final String externalRecordId;
    private final String checksum;
    private final Integer transferAttempts;
    private final Integer verificationAttempts;
    private final String manifestFile;
    private final OffsetDateTime eventDateTs;
    private final String errorCode;
    private final boolean responseCleaned;
    private final boolean updateRetention;
}
