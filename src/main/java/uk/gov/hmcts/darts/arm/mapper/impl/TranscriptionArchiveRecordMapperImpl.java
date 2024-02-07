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

    public static final String COMMENTS_DELIMITER = ",";
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
                .build();

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
            case CHECKSUM_KEY -> transcriptionDocument.getChecksum();
            case TRANSCRIPT_REQUEST_KEY -> transcriptionDocument.getTranscription().getRequestor();
            case TRANSCRIPT_TYPE_KEY -> transcriptionDocument.getFileType();
            case TRANSCRIPT_URGENCY_KEY -> transcriptionDocument.getTranscription().getTranscriptionUrgency().getDescription();
            case COMMENTS_KEY -> {
                String comments = null;
                if (CollectionUtils.isNotEmpty(transcriptionDocument.getTranscription().getTranscriptionCommentEntities())) {
                    comments = commentListToString(transcriptionDocument.getTranscription().getTranscriptionCommentEntities());
                }
                yield comments;
            }
            case CREATED_DATE_TIME_KEY -> {
                String createdDateTime = null;
                if (nonNull(transcriptionDocument.getUploadedDateTime())) {
                    createdDateTime = transcriptionDocument.getUploadedDateTime().format(dateTimeFormatter);
                }
                yield createdDateTime;
            }

            case UPLOADED_BY_KEY -> transcriptionDocument.getUploadedBy().getUserFullName();
            case START_DATE_TIME_KEY -> transcriptionDocument.getTranscription().getStartTime().format(dateTimeFormatter);
            case END_DATE_TIME_KEY -> transcriptionDocument.getTranscription().getEndTime().format(dateTimeFormatter);
            case COURTHOUSE_KEY -> transcriptionDocument.getTranscription().getCourtroom().getCourthouse().getCourthouseName();
            case COURTROOM_KEY -> transcriptionDocument.getTranscription().getCourtroom().getName();
            default -> null;
        };
    }

    private String commentListToString(List<TranscriptionCommentEntity> commentEntities) {
        List<String> comments = commentEntities.stream()
                .map(TranscriptionCommentEntity::getComment)
                .toList();
        return String.join(COMMENTS_DELIMITER, comments);
    }

    private Integer mapToInt(String key, TranscriptionDocumentEntity transcriptionDocument) {
        return switch (key) {
            case OBJECT_ID_KEY -> transcriptionDocument.getId();
            case PARENT_ID_KEY -> transcriptionDocument.getId();
            default -> null;
        };
    }
}
