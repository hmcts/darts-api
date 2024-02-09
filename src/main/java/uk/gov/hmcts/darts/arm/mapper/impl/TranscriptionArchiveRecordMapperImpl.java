package uk.gov.hmcts.darts.arm.mapper.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.TranscriptionCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.util.PropertyFileLoader;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveMapperValues.TRANSCRIPTION_REQUEST_AUTOMATIC;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveMapperValues.TRANSCRIPTION_REQUEST_MANUAL;
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
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CHECKSUM_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.COMMENTS_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.COURTHOUSE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.COURTROOM_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CREATED_DATE_TIME_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.END_DATE_TIME_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.FILE_TYPE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.HEARING_DATE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.OBJECT_ID_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.OBJECT_TYPE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.PARENT_ID_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.START_DATE_TIME_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.TRANSCRIPT_REQUEST_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.TRANSCRIPT_TYPE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.TRANSCRIPT_URGENCY_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.UPLOADED_BY_KEY;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranscriptionArchiveRecordMapperImpl implements TranscriptionArchiveRecordMapper {

    private static final String COMMENTS_DELIMITER = "|";

    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final CurrentTimeHelper currentTimeHelper;
    private Properties transcriptionRecordProperties;

    private DateTimeFormatter dateTimeFormatter;
    private DateTimeFormatter dateFormatter;


    @Override
    public TranscriptionArchiveRecord mapToTranscriptionArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, File archiveRecordFile) {
        dateTimeFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());
        dateFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateFormat());

        try {
            transcriptionRecordProperties = PropertyFileLoader.loadPropertiesFromFile(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile());
            TranscriptionDocumentEntity transcriptionDocument = externalObjectDirectory.getTranscriptionDocumentEntity();
            TranscriptionCreateArchiveRecordOperation transcriptionCreateArchiveRecordOperation = createArchiveRecordOperation(
                    externalObjectDirectory,
                    externalObjectDirectory.getId()
            );
            UploadNewFileRecord uploadNewFileRecord = createUploadNewFileRecord(transcriptionDocument, externalObjectDirectory.getId());
            return createTranscriptionArchiveRecord(transcriptionCreateArchiveRecordOperation, uploadNewFileRecord);
        } catch (IOException e) {
            log.error(
                    "Unable to read transcription property file {} - {}",
                    armDataManagementConfiguration.getTranscriptionRecordPropertiesFile(),
                    e.getMessage());
        }
        return null;
    }

    private TranscriptionArchiveRecord createTranscriptionArchiveRecord(TranscriptionCreateArchiveRecordOperation transcriptionCreateArchiveRecordOperation,
                                                                        UploadNewFileRecord uploadNewFileRecord) {
        return TranscriptionArchiveRecord.builder()
                .transcriptionCreateArchiveRecordOperation(transcriptionCreateArchiveRecordOperation)
                .uploadNewFileRecord(uploadNewFileRecord)
                .build();
    }

    private UploadNewFileRecord createUploadNewFileRecord(TranscriptionDocumentEntity transcriptionDocument, Integer relationId) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(relationId.toString());
        uploadNewFileRecord.setFileMetadata(createUploadNewFileRecordMetadata(transcriptionDocument));
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata(TranscriptionDocumentEntity transcriptionDocument) {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher(armDataManagementConfiguration.getPublisher());
        uploadNewFileRecordMetadata.setDzFilename(transcriptionDocument.getFileName());
        uploadNewFileRecordMetadata.setFileTag(transcriptionDocument.getFileType());
        return uploadNewFileRecordMetadata;
    }

    private TranscriptionCreateArchiveRecordOperation createArchiveRecordOperation(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                                   Integer relationId) {
        return TranscriptionCreateArchiveRecordOperation.builder()
                .relationId(relationId.toString())
                .recordMetadata(createArchiveRecordMetadata(externalObjectDirectory))
                .build();
    }

    @SuppressWarnings("java:S3776")
    private RecordMetadata createArchiveRecordMetadata(ExternalObjectDirectoryEntity externalObjectDirectory) {
        TranscriptionDocumentEntity transcriptionDocument = externalObjectDirectory.getTranscriptionDocumentEntity();

        RecordMetadata metadata = RecordMetadata.builder()
                .publisher(armDataManagementConfiguration.getPublisher())
                .recordClass(armDataManagementConfiguration.getTranscriptionRecordClass())
                .recordDate(currentTimeHelper.currentOffsetDateTime().format(dateTimeFormatter))
                .region(armDataManagementConfiguration.getRegion())
                .title(transcriptionDocument.getFileName())
                .clientId(String.valueOf(externalObjectDirectory.getId()))
                .build();

        String courthouse = getCourthouse(transcriptionDocument);
        String courtroom = getCourtroom(transcriptionDocument);
        if (nonNull(courthouse) && nonNull(courtroom)) {
            metadata.setContributor(courthouse + " & " + courtroom);
        }

        if (transcriptionRecordProperties.containsKey(BF_001_KEY)) {
            metadata.setBf001(mapToString(transcriptionRecordProperties.getProperty(BF_001_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_002_KEY)) {
            metadata.setBf002(mapToString(transcriptionRecordProperties.getProperty(BF_002_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_003_KEY)) {
            metadata.setBf003(mapToString(transcriptionRecordProperties.getProperty(BF_003_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_004_KEY)) {
            metadata.setBf004(mapToString(transcriptionRecordProperties.getProperty(BF_004_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_005_KEY)) {
            metadata.setBf005(mapToString(transcriptionRecordProperties.getProperty(BF_005_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_006_KEY)) {
            metadata.setBf006(mapToString(transcriptionRecordProperties.getProperty(BF_006_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_007_KEY)) {
            metadata.setBf007(mapToString(transcriptionRecordProperties.getProperty(BF_007_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_008_KEY)) {
            metadata.setBf008(mapToString(transcriptionRecordProperties.getProperty(BF_008_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_009_KEY)) {
            metadata.setBf009(mapToString(transcriptionRecordProperties.getProperty(BF_009_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_010_KEY)) {
            metadata.setBf010(mapToString(transcriptionRecordProperties.getProperty(BF_010_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_011_KEY)) {
            metadata.setBf011(mapToString(transcriptionRecordProperties.getProperty(BF_011_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_012_KEY)) {
            metadata.setBf012(mapToInt(transcriptionRecordProperties.getProperty(BF_012_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_013_KEY)) {
            metadata.setBf013(mapToInt(transcriptionRecordProperties.getProperty(BF_013_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_014_KEY)) {
            metadata.setBf014(mapToInt(transcriptionRecordProperties.getProperty(BF_014_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_015_KEY)) {
            metadata.setBf015(mapToInt(transcriptionRecordProperties.getProperty(BF_015_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_016_KEY)) {
            metadata.setBf016(mapToString(transcriptionRecordProperties.getProperty(BF_016_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_017_KEY)) {
            metadata.setBf017(mapToString(transcriptionRecordProperties.getProperty(BF_017_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_018_KEY)) {
            metadata.setBf018(mapToString(transcriptionRecordProperties.getProperty(BF_018_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_019_KEY)) {
            metadata.setBf019(mapToString(transcriptionRecordProperties.getProperty(BF_019_KEY), transcriptionDocument));
        }
        if (transcriptionRecordProperties.containsKey(BF_020_KEY)) {
            metadata.setBf020(mapToString(transcriptionRecordProperties.getProperty(BF_020_KEY), transcriptionDocument));
        }
        return metadata;
    }

    private String mapToString(String key, TranscriptionDocumentEntity transcriptionDocument) {
        return switch (key) {
            case OBJECT_TYPE_KEY -> ArchiveRecordType.TRANSCRIPTION_ARCHIVE_TYPE.getArchiveTypeDescription();
            case FILE_TYPE_KEY -> transcriptionDocument.getFileType();
            case HEARING_DATE_KEY -> getHearingDate(transcriptionDocument);
            case CHECKSUM_KEY -> transcriptionDocument.getChecksum();
            case TRANSCRIPT_REQUEST_KEY -> getTranscriptionRequest(transcriptionDocument);
            case TRANSCRIPT_TYPE_KEY -> getTranscriptionType(transcriptionDocument);
            case TRANSCRIPT_URGENCY_KEY -> getTranscriptionUrgency(transcriptionDocument);
            case COMMENTS_KEY -> getTranscriptionComments(transcriptionDocument);
            case CREATED_DATE_TIME_KEY -> getCreatedDateTime(transcriptionDocument);
            case UPLOADED_BY_KEY -> getUploadedBy(transcriptionDocument);
            case START_DATE_TIME_KEY -> getStartDateTime(transcriptionDocument);
            case END_DATE_TIME_KEY -> getEndDateTime(transcriptionDocument);
            case COURTHOUSE_KEY -> getCourthouse(transcriptionDocument);
            case COURTROOM_KEY -> getCourtroom(transcriptionDocument);
            default -> null;
        };
    }

    private String getEndDateTime(TranscriptionDocumentEntity transcriptionDocument) {
        String endDateTime = null;
        if (nonNull(transcriptionDocument.getTranscription()) && nonNull(transcriptionDocument.getTranscription().getEndTime())) {
            endDateTime = transcriptionDocument.getTranscription().getEndTime().format(dateTimeFormatter);
        }
        return endDateTime;
    }

    private String getStartDateTime(TranscriptionDocumentEntity transcriptionDocument) {
        String startDateTime = null;
        if (nonNull(transcriptionDocument.getTranscription()) && nonNull(transcriptionDocument.getTranscription().getStartTime())) {
            startDateTime = transcriptionDocument.getTranscription().getStartTime().format(dateTimeFormatter);
        }
        return startDateTime;
    }

    private static String getUploadedBy(TranscriptionDocumentEntity transcriptionDocument) {
        String uploadedBy = null;
        if (nonNull(transcriptionDocument.getUploadedBy())) {
            uploadedBy = transcriptionDocument.getUploadedBy().getUserFullName();
        }
        return uploadedBy;
    }

    private String getCreatedDateTime(TranscriptionDocumentEntity transcriptionDocument) {
        String createdDateTime = null;
        if (nonNull(transcriptionDocument.getUploadedDateTime())) {
            createdDateTime = transcriptionDocument.getUploadedDateTime().format(dateTimeFormatter);
        }
        return createdDateTime;
    }

    private String getTranscriptionComments(TranscriptionDocumentEntity transcriptionDocument) {
        String comments = null;
        if (nonNull(transcriptionDocument.getTranscription())
                && CollectionUtils.isNotEmpty(transcriptionDocument.getTranscription().getTranscriptionCommentEntities())) {
            comments = commentListToString(transcriptionDocument.getTranscription().getTranscriptionCommentEntities());
        }
        return comments;
    }

    private static String getTranscriptionUrgency(TranscriptionDocumentEntity transcriptionDocument) {
        String transcriptionUrgency = null;
        if (nonNull(transcriptionDocument.getTranscription()) && nonNull(transcriptionDocument.getTranscription().getTranscriptionUrgency())) {
            transcriptionUrgency = transcriptionDocument.getTranscription().getTranscriptionUrgency().getDescription();
        }
        return transcriptionUrgency;
    }

    private static String getTranscriptionType(TranscriptionDocumentEntity transcriptionDocument) {
        String transcriptionType = null;
        if (nonNull(transcriptionDocument.getTranscription()) && nonNull(transcriptionDocument.getTranscription().getTranscriptionType())) {
            transcriptionType = transcriptionDocument.getTranscription().getTranscriptionType().getDescription();
        }
        return transcriptionType;
    }

    private static String getTranscriptionRequest(TranscriptionDocumentEntity transcriptionDocument) {
        String transcriptRquest = null;
        if (nonNull(transcriptionDocument.getTranscription())) {
            transcriptRquest = transcriptionDocument.getTranscription().getIsManualTranscription() == Boolean.TRUE
                    ? TRANSCRIPTION_REQUEST_MANUAL : TRANSCRIPTION_REQUEST_AUTOMATIC;
        }
        return transcriptRquest;
    }

    private String getHearingDate(TranscriptionDocumentEntity transcriptionDocument) {
        String hearingDate = null;
        if (nonNull(transcriptionDocument.getTranscription()) && nonNull(transcriptionDocument.getTranscription().getHearingDate())) {
            hearingDate = transcriptionDocument.getTranscription().getHearingDate().format(dateFormatter);
        }
        return hearingDate;
    }

    private static String getCourtroom(TranscriptionDocumentEntity transcriptionDocument) {
        String courtroom = null;
        if (nonNull(transcriptionDocument.getTranscription()) && nonNull(transcriptionDocument.getTranscription().getCourtroom())) {
            courtroom = transcriptionDocument.getTranscription().getCourtroom().getName();
        }
        return courtroom;
    }

    private static String getCourthouse(TranscriptionDocumentEntity transcriptionDocument) {
        String courthouse = null;
        if (nonNull(transcriptionDocument.getTranscription())
                && nonNull(transcriptionDocument.getTranscription().getCourtroom())
                && nonNull(transcriptionDocument.getTranscription().getCourtroom().getCourthouse())) {
            courthouse = transcriptionDocument.getTranscription().getCourtroom().getCourthouse().getCourthouseName();
        }
        return courthouse;
    }

    private String commentListToString(List<TranscriptionCommentEntity> commentEntities) {
        List<String> comments = commentEntities.stream()
                .map(TranscriptionCommentEntity::getComment)
                .toList();
        return String.join(COMMENTS_DELIMITER, comments);
    }

    private Integer mapToInt(String key, TranscriptionDocumentEntity transcriptionDocument) {
        return switch (key) {
            case OBJECT_ID_KEY, PARENT_ID_KEY -> transcriptionDocument.getId();
            default -> null;
        };
    }
}
