package uk.gov.hmcts.darts.arm.mapper.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.mapper.CaseArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.CaseArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.CaseCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.util.PropertyFileLoader;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Properties;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.UPLOAD_NEW_FILE;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CASE_NUMBERS_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CHECKSUM_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.COURTHOUSE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CREATED_DATE_TIME_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.FILE_TYPE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.HEARING_DATE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.OBJECT_ID_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.OBJECT_TYPE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.PARENT_ID_KEY;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({
    "PMD.GodClass",
    "PMD.CyclomaticComplexity",
    "PMD.TooManyMethods"//TODO - refactor to reduce methods when this class is next edited
})
public class CaseArchiveRecordMapperImpl extends BaseArchiveRecordMapper implements CaseArchiveRecordMapper {

    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    private final CurrentTimeHelper currentTimeHelper;
    private Properties caseRecordProperties;

    private DateTimeFormatter dateTimeFormatter;

    @Override
    public CaseArchiveRecord mapToCaseArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, String rawFilename) {
        dateTimeFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());

        try {
            loadCaseProperties();
            CaseDocumentEntity caseDocument = externalObjectDirectory.getCaseDocument();
            CaseCreateArchiveRecordOperation caseCreateArchiveRecordOperation = createArchiveRecordOperation(externalObjectDirectory);
            UploadNewFileRecord uploadNewFileRecord = createUploadNewFileRecord(caseDocument, externalObjectDirectory.getId(), rawFilename);
            return createCaseArchiveRecord(caseCreateArchiveRecordOperation, uploadNewFileRecord);
        } catch (IOException e) {
            log.error(
                "Unable to read case property file {} - {}",
                armDataManagementConfiguration.getCaseRecordPropertiesFile(),
                e.getMessage());
        }
        return null;
    }

    private void loadCaseProperties() throws IOException {
        if (isNull(caseRecordProperties) || caseRecordProperties.isEmpty()) {
            caseRecordProperties = PropertyFileLoader.loadPropertiesFromFile(armDataManagementConfiguration.getCaseRecordPropertiesFile());
        }
        if (caseRecordProperties.isEmpty()) {
            log.warn("Failed to load property file {}", armDataManagementConfiguration.getCaseRecordPropertiesFile());
        }
    }

    private CaseArchiveRecord createCaseArchiveRecord(CaseCreateArchiveRecordOperation caseCreateArchiveRecordOperation,
                                                      UploadNewFileRecord uploadNewFileRecord) {
        return CaseArchiveRecord.builder()
            .caseCreateArchiveRecordOperation(caseCreateArchiveRecordOperation)
            .uploadNewFileRecord(uploadNewFileRecord)
            .build();
    }

    private UploadNewFileRecord createUploadNewFileRecord(CaseDocumentEntity caseDocument, Long relationId, String rawFilename) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(String.valueOf(relationId));
        uploadNewFileRecord.setFileMetadata(createUploadNewFileRecordMetadata(caseDocument, rawFilename));
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata(CaseDocumentEntity caseDocument, String rawFilename) {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher(armDataManagementConfiguration.getPublisher());
        uploadNewFileRecordMetadata.setDzFilename(rawFilename);
        uploadNewFileRecordMetadata.setFileTag(FilenameUtils.getExtension(caseDocument.getFileName()));
        return uploadNewFileRecordMetadata;
    }

    private CaseCreateArchiveRecordOperation createArchiveRecordOperation(ExternalObjectDirectoryEntity externalObjectDirectory) {
        return CaseCreateArchiveRecordOperation.builder()
            .relationId(String.valueOf(externalObjectDirectory.getId()))
            .recordMetadata(createArchiveRecordMetadata(externalObjectDirectory))
            .build();
    }

    private RecordMetadata createArchiveRecordMetadata(ExternalObjectDirectoryEntity externalObjectDirectory) {
        CaseDocumentEntity caseDocument = externalObjectDirectory.getCaseDocument();
        OffsetDateTime retainUntilTs = caseDocument.getRetainUntilTs();
        RecordMetadata metadata = RecordMetadata.builder()
            .publisher(armDataManagementConfiguration.getPublisher())
            .recordClass(armDataManagementConfiguration.getCaseRecordClass())
            .recordDate(formatDateTime(currentTimeHelper.currentOffsetDateTime()))
            .eventDate(formatDateTime(nonNull(retainUntilTs)
                                          ? retainUntilTs.minusYears(armDataManagementConfiguration.getEventDateAdjustmentYears())
                                          : caseDocument.getCreatedDateTime()))
            .region(armDataManagementConfiguration.getRegion())
            .title(caseDocument.getFileName())
            .clientId(String.valueOf(externalObjectDirectory.getId()))
            .build();

        String courthouse = getCourthouse(caseDocument);
        if (nonNull(courthouse)) {
            metadata.setContributor(courthouse);
        }

        if (nonNull(caseDocument.getRetConfReason())) {
            metadata.setRetentionConfidenceReason(caseDocument.getRetConfReason());
        }

        if (nonNull(caseDocument.getRetConfScore())) {
            metadata.setRetentionConfidenceScore(caseDocument.getRetConfScore().getId());
        }

        setMetadataProperties(metadata, caseDocument);
        return metadata;
    }

    private void setMetadataProperties(RecordMetadata metadata, CaseDocumentEntity caseDocument) {
        for (String key : caseRecordProperties.stringPropertyNames()) {
            String value = mapToString(caseRecordProperties.getProperty(key), caseDocument);
            if (value != null) {
                processStringMetadataProperties(metadata, key, value);
            } else {
                Long longValue = mapToLong(caseRecordProperties.getProperty(key), caseDocument);
                processIntMetadataProperties(metadata, key, longValue);
            }
        }
    }

    private String mapToString(String key, CaseDocumentEntity caseDocument) {
        return switch (key) {
            case OBJECT_TYPE_KEY -> ArchiveRecordType.CASE_ARCHIVE_TYPE.getArchiveTypeDescription();
            case CASE_NUMBERS_KEY -> getCaseNumbers(caseDocument);
            case FILE_TYPE_KEY -> caseDocument.getFileType();
            case HEARING_DATE_KEY -> getHearingDate(caseDocument);
            case CHECKSUM_KEY -> caseDocument.getChecksum();
            case CREATED_DATE_TIME_KEY -> formatDateTime(caseDocument.getCreatedDateTime());
            case COURTHOUSE_KEY -> getCourthouse(caseDocument);
            default -> null;
        };
    }

    private String formatDateTime(OffsetDateTime offsetDateTime) {
        String dateTime = null;
        if (nonNull(offsetDateTime)) {
            dateTime = offsetDateTime.format(dateTimeFormatter);
        }
        return dateTime;
    }

    private String getHearingDate(CaseDocumentEntity caseDocument) {
        String hearingDate = null;
        if (CollectionUtils.isNotEmpty(caseDocument.getCourtCase().getHearings())) {
            hearingDate = OffsetDateTime.of(caseDocument.getCourtCase().getHearings().get(0).getHearingDate().atTime(0, 0, 0),
                                            ZoneOffset.UTC).format(dateTimeFormatter);
        }
        return hearingDate;
    }

    private String getCaseNumbers(CaseDocumentEntity caseDocument) {
        String cases = null;
        if (nonNull(caseDocument.getCourtCase())) {
            cases = caseDocument.getCourtCase().getCaseNumber();
        }
        return cases;
    }

    private static String getCourthouse(CaseDocumentEntity caseDocument) {
        String courthouse = null;
        if (nonNull(caseDocument.getCourtCase())) {
            courthouse = caseDocument.getCourtCase().getCourthouse().getDisplayName();
        }
        return courthouse;
    }

    private Long mapToLong(String key, CaseDocumentEntity caseDocument) {
        return switch (key) {
            case OBJECT_ID_KEY -> caseDocument.getId();
            case PARENT_ID_KEY -> Optional.ofNullable(caseDocument.getCourtCase().getId()).map(Integer::longValue).orElse(null);
            default -> null;
        };
    }
}
