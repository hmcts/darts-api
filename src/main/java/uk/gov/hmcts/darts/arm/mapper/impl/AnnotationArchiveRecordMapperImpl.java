package uk.gov.hmcts.darts.arm.mapper.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.AnnotationCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.util.PropertyFileLoader;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.COURTROOM_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CREATED_DATE_TIME_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.FILE_TYPE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.HEARING_DATE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.OBJECT_ID_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.OBJECT_TYPE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.PARENT_ID_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.UPLOADED_BY_KEY;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnnotationArchiveRecordMapperImpl implements AnnotationArchiveRecordMapper {

    private static final String CASE_LIST_DELIMITER = "|";
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final CurrentTimeHelper currentTimeHelper;
    private Properties annotationRecordProperties;

    private DateTimeFormatter dateTimeFormatter;
    private DateTimeFormatter dateFormatter;


    @Override
    public AnnotationArchiveRecord mapToAnnotationArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, File archiveRecordFile) {
        dateTimeFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());
        dateFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateFormat());

        try {
            loadAnnotationProperties();
            AnnotationDocumentEntity annotationDocument = externalObjectDirectory.getAnnotationDocumentEntity();
            AnnotationCreateArchiveRecordOperation annotationCreateArchiveRecordOperation = createArchiveRecordOperation(externalObjectDirectory);
            UploadNewFileRecord uploadNewFileRecord = createUploadNewFileRecord(annotationDocument, externalObjectDirectory.getId(), archiveRecordFile);
            return createAnnotationArchiveRecord(annotationCreateArchiveRecordOperation, uploadNewFileRecord);
        } catch (IOException e) {
            log.error(
                "Unable to read annotation property file {} - {}",
                armDataManagementConfiguration.getAnnotationRecordPropertiesFile(),
                e.getMessage());
        }
        return null;
    }

    private void loadAnnotationProperties() throws IOException {
        if (isNull(annotationRecordProperties) || annotationRecordProperties.isEmpty()) {
            annotationRecordProperties = PropertyFileLoader.loadPropertiesFromFile(armDataManagementConfiguration.getAnnotationRecordPropertiesFile());
        }
    }

    private AnnotationArchiveRecord createAnnotationArchiveRecord(AnnotationCreateArchiveRecordOperation annotationCreateArchiveRecordOperation,
                                                                  UploadNewFileRecord uploadNewFileRecord) {
        return AnnotationArchiveRecord.builder()
            .annotationCreateArchiveRecordOperation(annotationCreateArchiveRecordOperation)
            .uploadNewFileRecord(uploadNewFileRecord)
            .build();
    }

    private AnnotationCreateArchiveRecordOperation createArchiveRecordOperation(ExternalObjectDirectoryEntity externalObjectDirectory) {
        return AnnotationCreateArchiveRecordOperation.builder()
            .relationId(String.valueOf(externalObjectDirectory.getId()))
            .recordMetadata(createArchiveRecordMetadata(externalObjectDirectory))
            .build();
    }

    private RecordMetadata createArchiveRecordMetadata(ExternalObjectDirectoryEntity externalObjectDirectory) {
        AnnotationDocumentEntity annotationDocument = externalObjectDirectory.getAnnotationDocumentEntity();
        RecordMetadata metadata = RecordMetadata.builder()
            .publisher(armDataManagementConfiguration.getPublisher())
            .recordClass(armDataManagementConfiguration.getMediaRecordClass())
            .recordDate(currentTimeHelper.currentOffsetDateTime().format(dateTimeFormatter))
            .region(armDataManagementConfiguration.getRegion())
            .title(annotationDocument.getFileName())
            .clientId(String.valueOf(externalObjectDirectory.getId()))
            .build();

        String courthouse = getCourthouse(annotationDocument);
        String courtroom = getCourtroom(annotationDocument);
        if (nonNull(courthouse) && nonNull(courtroom)) {
            metadata.setContributor(courthouse + " & " + courtroom);
        }

        if (annotationRecordProperties.containsKey(BF_001_KEY)) {
            metadata.setBf001(mapToString(annotationRecordProperties.getProperty(BF_001_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_002_KEY)) {
            metadata.setBf002(mapToString(annotationRecordProperties.getProperty(BF_002_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_003_KEY)) {
            metadata.setBf003(mapToString(annotationRecordProperties.getProperty(BF_003_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_004_KEY)) {
            metadata.setBf004(mapToString(annotationRecordProperties.getProperty(BF_004_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_005_KEY)) {
            metadata.setBf005(mapToString(annotationRecordProperties.getProperty(BF_005_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_006_KEY)) {
            metadata.setBf006(mapToString(annotationRecordProperties.getProperty(BF_006_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_007_KEY)) {
            metadata.setBf007(mapToString(annotationRecordProperties.getProperty(BF_007_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_008_KEY)) {
            metadata.setBf008(mapToString(annotationRecordProperties.getProperty(BF_008_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_009_KEY)) {
            metadata.setBf009(mapToString(annotationRecordProperties.getProperty(BF_009_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_010_KEY)) {
            metadata.setBf010(mapToString(annotationRecordProperties.getProperty(BF_010_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_011_KEY)) {
            metadata.setBf011(mapToString(annotationRecordProperties.getProperty(BF_011_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_012_KEY)) {
            metadata.setBf012(mapToInt(annotationRecordProperties.getProperty(BF_012_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_013_KEY)) {
            metadata.setBf013(mapToInt(annotationRecordProperties.getProperty(BF_013_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_014_KEY)) {
            metadata.setBf014(mapToInt(annotationRecordProperties.getProperty(BF_014_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_015_KEY)) {
            metadata.setBf015(mapToInt(annotationRecordProperties.getProperty(BF_015_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_016_KEY)) {
            metadata.setBf016(mapToString(annotationRecordProperties.getProperty(BF_016_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_017_KEY)) {
            metadata.setBf017(mapToString(annotationRecordProperties.getProperty(BF_017_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_018_KEY)) {
            metadata.setBf018(mapToString(annotationRecordProperties.getProperty(BF_018_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_019_KEY)) {
            metadata.setBf019(mapToString(annotationRecordProperties.getProperty(BF_019_KEY), annotationDocument));
        }
        if (annotationRecordProperties.containsKey(BF_020_KEY)) {
            metadata.setBf020(mapToString(annotationRecordProperties.getProperty(BF_020_KEY), annotationDocument));
        }
        return metadata;
    }

    private String mapToString(String key, AnnotationDocumentEntity annotationDocument) {
        return switch (key) {
            case OBJECT_TYPE_KEY -> ArchiveRecordType.MEDIA_ARCHIVE_TYPE.getArchiveTypeDescription();
            case CASE_NUMBERS_KEY -> getCaseNumbers(annotationDocument);
            case FILE_TYPE_KEY -> annotationDocument.getFileType();
            case HEARING_DATE_KEY -> getHearingDate(annotationDocument);
            case CHECKSUM_KEY -> annotationDocument.getChecksum();
            case CREATED_DATE_TIME_KEY -> formatDateTime(annotationDocument.getUploadedDateTime());
            case UPLOADED_BY_KEY -> getUploadedBy(annotationDocument);
            case COURTHOUSE_KEY -> getCourthouse(annotationDocument);
            case COURTROOM_KEY -> getCourtroom(annotationDocument);
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

    private String getHearingDate(AnnotationDocumentEntity annotationDocument) {
        String hearingDate = null;
        if (CollectionUtils.isNotEmpty(annotationDocument.getAnnotation().getHearingList())) {
            hearingDate = annotationDocument.getAnnotation().getHearingList().get(0).getHearingDate().format(dateFormatter);
        }
        return hearingDate;
    }

    private String getCaseNumbers(AnnotationDocumentEntity annotationDocument) {
        String cases = null;
        if (nonNull(annotationDocument.getAnnotation().getHearingList())) {
            List<HearingEntity> hearings = annotationDocument.getAnnotation().getHearingList();
            List<String> caseNumbers = hearings
                .stream()
                .map(HearingEntity::getCourtCase)
                .map(CourtCaseEntity::getCaseNumber)
                .toList();
            cases = caseListToString(caseNumbers);
        }
        return cases;
    }

    private static String getCourthouse(AnnotationDocumentEntity annotationDocument) {
        String courthouse = null;
        if (CollectionUtils.isNotEmpty(annotationDocument.getAnnotation().getHearingList())) {
            courthouse = annotationDocument.getAnnotation().getHearingList().get(0).getCourtCase().getCourthouse().getCourthouseName();
        }
        return courthouse;
    }

    private static String getCourtroom(AnnotationDocumentEntity annotationDocument) {
        String courtroom = null;
        if (CollectionUtils.isNotEmpty(annotationDocument.getAnnotation().getHearingList())) {
            courtroom = annotationDocument.getAnnotation().getHearingList().get(0).getCourtroom().getName();
        }
        return courtroom;
    }

    private static String getUploadedBy(AnnotationDocumentEntity annotationDocument) {
        String uploadedBy = null;
        if (nonNull(annotationDocument.getUploadedBy())) {
            uploadedBy = String.valueOf(annotationDocument.getUploadedBy().getId());
        }
        return uploadedBy;
    }

    private Integer mapToInt(String key, AnnotationDocumentEntity annotationDocument) {
        return switch (key) {
            case OBJECT_ID_KEY, PARENT_ID_KEY -> annotationDocument.getId();
            default -> null;
        };
    }

    private String caseListToString(List<String> caseNumberList) {
        return String.join(CASE_LIST_DELIMITER, caseNumberList);
    }

    private UploadNewFileRecord createUploadNewFileRecord(AnnotationDocumentEntity annotationDocument, Integer relationId, File archiveRecordFile) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(String.valueOf(relationId));
        uploadNewFileRecord.setFileMetadata(createUploadNewFileRecordMetadata(annotationDocument, archiveRecordFile));
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata(AnnotationDocumentEntity annotationDocument, File archiveRecordFile) {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher(armDataManagementConfiguration.getPublisher());
        uploadNewFileRecordMetadata.setDzFilename(archiveRecordFile.getName());
        uploadNewFileRecordMetadata.setFileTag(annotationDocument.getFileType());
        return uploadNewFileRecordMetadata;
    }
}
