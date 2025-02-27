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
import java.util.Properties;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.UPLOAD_NEW_FILE;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_001_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_002_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_003_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_004_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_005_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_006_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_007_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_008_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_009_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_010_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_011_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_012_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_013_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_014_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_015_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_016_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_017_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_018_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_019_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyKeys.BF_020_KEY;
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
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity"})
public class CaseArchiveRecordMapperImpl implements CaseArchiveRecordMapper {

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

    private UploadNewFileRecord createUploadNewFileRecord(CaseDocumentEntity caseDocument, Integer relationId, String rawFilename) {
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
                switch (key) {
                    case BF_001_KEY -> metadata.setBf001(value);
                    case BF_002_KEY -> metadata.setBf002(value);
                    case BF_003_KEY -> metadata.setBf003(value);
                    case BF_004_KEY -> metadata.setBf004(value);
                    case BF_005_KEY -> metadata.setBf005(value);
                    case BF_006_KEY -> metadata.setBf006(value);
                    case BF_007_KEY -> metadata.setBf007(value);
                    case BF_008_KEY -> metadata.setBf008(value);
                    case BF_009_KEY -> metadata.setBf009(value);
                    case BF_010_KEY -> metadata.setBf010(value);
                    case BF_011_KEY -> metadata.setBf011(value);
                    case BF_016_KEY -> metadata.setBf016(value);
                    case BF_017_KEY -> metadata.setBf017(value);
                    case BF_018_KEY -> metadata.setBf018(value);
                    case BF_019_KEY -> metadata.setBf019(value);
                    case BF_020_KEY -> metadata.setBf020(value);
                    default -> log.warn("Case archive record unknown property key: {}", key);
                }
            } else {
                Integer intValue = mapToInt(caseRecordProperties.getProperty(key), caseDocument);
                if (intValue != null) {
                    switch (key) {
                        case BF_012_KEY -> metadata.setBf012(intValue);
                        case BF_013_KEY -> metadata.setBf013(intValue);
                        case BF_014_KEY -> metadata.setBf014(intValue);
                        case BF_015_KEY -> metadata.setBf015(intValue);
                        default -> log.warn("Case archive record unknown integer property key: {}", key);
                    }
                }
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

    private Integer mapToInt(String key, CaseDocumentEntity caseDocument) {
        return switch (key) {
            case OBJECT_ID_KEY -> caseDocument.getId();
            case PARENT_ID_KEY -> caseDocument.getCourtCase().getId();
            default -> null;
        };
    }
}
