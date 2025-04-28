package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;

import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class ExternalObjectDirectoryCaseDocument extends CreatedModifiedCaseDocument {

    private final Long id;
    private final Long media;
    private final Long transcriptionDocument;
    private final Long annotationDocument;
    private final Long caseDocument;
    private final ObjectRecordStatusEntity status;
    private final ExternalLocationTypeEntity externalLocationType;
    private final String externalLocation;
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
