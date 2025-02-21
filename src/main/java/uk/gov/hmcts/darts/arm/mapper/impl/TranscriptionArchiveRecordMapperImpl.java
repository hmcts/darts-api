package uk.gov.hmcts.darts.arm.mapper.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.TranscriptionCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.util.PropertyFileLoader;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static java.util.Objects.isNull;
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
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CASE_NUMBERS_KEY;
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
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity"})
public class TranscriptionArchiveRecordMapperImpl implements TranscriptionArchiveRecordMapper {

    private static final String CASE_LIST_DELIMITER = "|";

    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    private final CurrentTimeHelper currentTimeHelper;
    private Properties transcriptionRecordProperties;

    private DateTimeFormatter dateTimeFormatter;


    @Override
    public TranscriptionArchiveRecord mapToTranscriptionArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                      String rawFilename) {
        dateTimeFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());

        try {
            loadTranscriptionProperties();
            TranscriptionDocumentEntity transcriptionDocument = externalObjectDirectory.getTranscriptionDocumentEntity();
            TranscriptionCreateArchiveRecordOperation transcriptionCreateArchiveRecordOperation = createArchiveRecordOperation(externalObjectDirectory);
            UploadNewFileRecord uploadNewFileRecord = createUploadNewFileRecord(transcriptionDocument, externalObjectDirectory.getId(), rawFilename);
            return createTranscriptionArchiveRecord(transcriptionCreateArchiveRecordOperation, uploadNewFileRecord);
        } catch (IOException e) {
            log.error(
                "Unable to read transcription property file {} - {}",
                armDataManagementConfiguration.getTranscriptionRecordPropertiesFile(),
                e.getMessage());
        }
        return null;
    }

    private void loadTranscriptionProperties() throws IOException {
        if (isNull(transcriptionRecordProperties) || transcriptionRecordProperties.isEmpty()) {
            transcriptionRecordProperties =
                PropertyFileLoader.loadPropertiesFromFile(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile());
        }
        if (transcriptionRecordProperties.isEmpty()) {
            log.warn("Failed to load property file {}", armDataManagementConfiguration.getTranscriptionRecordPropertiesFile());
        }
    }

    private TranscriptionArchiveRecord createTranscriptionArchiveRecord(TranscriptionCreateArchiveRecordOperation transcriptionCreateArchiveRecordOperation,
                                                                        UploadNewFileRecord uploadNewFileRecord) {
        return TranscriptionArchiveRecord.builder()
            .transcriptionCreateArchiveRecordOperation(transcriptionCreateArchiveRecordOperation)
            .uploadNewFileRecord(uploadNewFileRecord)
            .build();
    }

    private UploadNewFileRecord createUploadNewFileRecord(TranscriptionDocumentEntity transcriptionDocument, Integer relationId, String rawFilename) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(relationId.toString());
        uploadNewFileRecord.setFileMetadata(createUploadNewFileRecordMetadata(transcriptionDocument, rawFilename));
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata(TranscriptionDocumentEntity transcriptionDocument, String rawFilename) {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher(armDataManagementConfiguration.getPublisher());
        uploadNewFileRecordMetadata.setDzFilename(rawFilename);
        uploadNewFileRecordMetadata.setFileTag(FilenameUtils.getExtension(transcriptionDocument.getFileName()));
        return uploadNewFileRecordMetadata;
    }

    private TranscriptionCreateArchiveRecordOperation createArchiveRecordOperation(ExternalObjectDirectoryEntity externalObjectDirectory) {
        return TranscriptionCreateArchiveRecordOperation.builder()
            .relationId(String.valueOf(externalObjectDirectory.getId()))
            .recordMetadata(createArchiveRecordMetadata(externalObjectDirectory))
            .build();
    }

    @SuppressWarnings({"java:S3776", "PMD.CyclomaticComplexity", "PMD.CognitiveComplexity", "PMD.NPathComplexity"})
    private RecordMetadata createArchiveRecordMetadata(ExternalObjectDirectoryEntity externalObjectDirectory) {
        TranscriptionDocumentEntity transcriptionDocument = externalObjectDirectory.getTranscriptionDocumentEntity();
        OffsetDateTime retainUntilTs = transcriptionDocument.getRetainUntilTs();
        RecordMetadata metadata = RecordMetadata.builder()
            .publisher(armDataManagementConfiguration.getPublisher())
            .recordClass(armDataManagementConfiguration.getTranscriptionRecordClass())
            .recordDate(formatDateTime(currentTimeHelper.currentOffsetDateTime()))
            .eventDate(formatDateTime(nonNull(retainUntilTs)
                                          ? retainUntilTs.minusYears(armDataManagementConfiguration.getEventDateAdjustmentYears())
                                          : transcriptionDocument.getUploadedDateTime()))
            .region(armDataManagementConfiguration.getRegion())
            .title(transcriptionDocument.getFileName())
            .clientId(String.valueOf(externalObjectDirectory.getId()))
            .build();

        String courthouse = getCourthouse(transcriptionDocument);
        String courtroom = getCourtroom(transcriptionDocument);
        if (nonNull(courthouse) && nonNull(courtroom)) {
            metadata.setContributor(courthouse + " & " + courtroom);
        }

        if (nonNull(transcriptionDocument.getRetConfReason())) {
            metadata.setRetentionConfidenceReason(transcriptionDocument.getRetConfReason());
        }

        if (nonNull(transcriptionDocument.getRetConfScore())) {
            metadata.setRetentionConfidenceScore(transcriptionDocument.getRetConfScore().getId());
        }

        setMetadataProperties(metadata, transcriptionDocument);
        return metadata;
    }

    private void setMetadataProperties(RecordMetadata metadata, TranscriptionDocumentEntity transcriptionDocument) {
        for (String key : transcriptionRecordProperties.stringPropertyNames()) {
            String value = mapToString(transcriptionRecordProperties.getProperty(key), transcriptionDocument);
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
                    case BF_012_KEY -> metadata.setBf012(mapToInt(value, transcriptionDocument));
                    case BF_013_KEY -> metadata.setBf013(mapToInt(value, transcriptionDocument));
                    case BF_014_KEY -> metadata.setBf014(mapToInt(value, transcriptionDocument));
                    case BF_015_KEY -> metadata.setBf015(mapToInt(value, transcriptionDocument));
                    case BF_016_KEY -> metadata.setBf016(value);
                    case BF_017_KEY -> metadata.setBf017(value);
                    case BF_018_KEY -> metadata.setBf018(value);
                    case BF_019_KEY -> metadata.setBf019(value);
                    case BF_020_KEY -> metadata.setBf020(value);
                    default -> {
                        // ignore unknown properties - comment to fix PMD warning
                    }
                }
            }
        }
    }

    private String mapToString(String key, TranscriptionDocumentEntity transcriptionDocument) {
        return switch (key) {
            case OBJECT_TYPE_KEY -> ArchiveRecordType.TRANSCRIPTION_ARCHIVE_TYPE.getArchiveTypeDescription();
            case CASE_NUMBERS_KEY -> getCaseNumbers(transcriptionDocument);
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

    private String getCaseNumbers(TranscriptionDocumentEntity transcriptionDocumentEntity) {
        List<CourtCaseEntity> cases = transcriptionDocumentEntity.getTranscription().getAssociatedCourtCases();
        if (cases.isEmpty()) {
            return null;
        } else if (cases.size() == 1) {
            return cases.getFirst().getCaseNumber();
        } else {
            List<String> caseNumbers = cases
                .stream()
                .map(CourtCaseEntity::getCaseNumber)
                .toList();
            return caseListToString(caseNumbers);
        }
    }

    private String caseListToString(List<String> caseNumberList) {
        return String.join(CASE_LIST_DELIMITER, caseNumberList);
    }

    private String getEndDateTime(TranscriptionDocumentEntity transcriptionDocument) {
        String endDateTime = null;
        if (nonNull(transcriptionDocument.getTranscription().getEndTime())) {
            endDateTime = transcriptionDocument.getTranscription().getEndTime().format(dateTimeFormatter);
        }
        return endDateTime;
    }

    private String getStartDateTime(TranscriptionDocumentEntity transcriptionDocument) {
        String startDateTime = null;
        if (nonNull(transcriptionDocument.getTranscription().getStartTime())) {
            startDateTime = transcriptionDocument.getTranscription().getStartTime().format(dateTimeFormatter);
        }
        return startDateTime;
    }

    private static String getUploadedBy(TranscriptionDocumentEntity transcriptionDocument) {
        String uploadedBy = null;
        if (nonNull(transcriptionDocument.getUploadedBy())) {
            uploadedBy = String.valueOf(transcriptionDocument.getUploadedBy().getId());
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
        if (CollectionUtils.isNotEmpty(transcriptionDocument.getTranscription().getTranscriptionCommentEntities())) {
            Optional<TranscriptionCommentEntity> transcriptionCommentEntity = transcriptionDocument
                .getTranscription()
                .getTranscriptionCommentEntities()
                .stream()
                .filter(this::isTranscriptionCommentRequested)
                .findFirst();
            if (transcriptionCommentEntity.isPresent()) {
                comments = transcriptionCommentEntity.get().getComment();
            }
        }
        return comments;
    }

    private boolean isTranscriptionCommentRequested(TranscriptionCommentEntity transcriptionComment) {
        boolean isTranscriptionCommentRequested = false;
        if (nonNull(transcriptionComment.getTranscriptionWorkflow())
            && nonNull(transcriptionComment.getTranscriptionWorkflow().getTranscriptionStatus())) {
            isTranscriptionCommentRequested = REQUESTED.getId().equals(transcriptionComment.getTranscriptionWorkflow().getTranscriptionStatus().getId());
        }
        return isTranscriptionCommentRequested;
    }

    private static String getTranscriptionUrgency(TranscriptionDocumentEntity transcriptionDocument) {
        String transcriptionUrgency = null;
        if (nonNull(transcriptionDocument.getTranscription().getTranscriptionUrgency())) {
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
            transcriptRquest = Boolean.TRUE.equals(transcriptionDocument.getTranscription().getIsManualTranscription())
                ? TRANSCRIPTION_REQUEST_MANUAL : TRANSCRIPTION_REQUEST_AUTOMATIC;
        }
        return transcriptRquest;
    }

    private String getHearingDate(TranscriptionDocumentEntity transcriptionDocument) {
        String hearingDate = null;
        if (nonNull(transcriptionDocument.getTranscription().getHearingDate())) {
            hearingDate = OffsetDateTime.of(transcriptionDocument.getTranscription().getHearingDate().atTime(0, 0, 0),
                                            ZoneOffset.UTC).format(dateTimeFormatter);
        } else if (CollectionUtils.isNotEmpty(transcriptionDocument.getTranscription().getHearings())) {
            hearingDate = OffsetDateTime.of(transcriptionDocument.getTranscription().getHearings().getFirst().getHearingDate().atTime(0, 0, 0),
                                            ZoneOffset.UTC).format(dateTimeFormatter);
        }
        return hearingDate;
    }

    private static String getCourtroom(TranscriptionDocumentEntity transcriptionDocument) {
        String courtroom = null;
        if (nonNull(transcriptionDocument.getTranscription().getHearing())
            && nonNull(transcriptionDocument.getTranscription().getHearing().getCourtroom())) {
            courtroom = transcriptionDocument.getTranscription().getHearing().getCourtroom().getName();
        } else if (nonNull(transcriptionDocument.getTranscription().getCourtroom())) {
            courtroom = transcriptionDocument.getTranscription().getCourtroom().getName();
        }
        return courtroom;
    }

    private static String getCourthouse(TranscriptionDocumentEntity transcriptionDocument) {
        String courthouse = null;
        if (nonNull(transcriptionDocument.getTranscription().getHearing())
            && nonNull(transcriptionDocument.getTranscription().getHearing().getCourtroom())
            && nonNull(transcriptionDocument.getTranscription().getHearing().getCourtroom().getCourthouse())) {
            courthouse = transcriptionDocument.getTranscription().getHearing().getCourtroom().getCourthouse().getDisplayName();
        } else if (nonNull(transcriptionDocument.getTranscription().getCourtCase().getCourthouse())
            && nonNull(transcriptionDocument.getTranscription().getCourtCase().getCourthouse())) {
            courthouse = transcriptionDocument.getTranscription().getCourtCase().getCourthouse().getDisplayName();
        }
        return courthouse;
    }

    private Integer mapToInt(String key, TranscriptionDocumentEntity transcriptionDocument) {
        return switch (key) {
            case OBJECT_ID_KEY -> transcriptionDocument.getId();
            case PARENT_ID_KEY -> transcriptionDocument.getTranscription().getId();
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
}
