package uk.gov.hmcts.darts.arm.mapper.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.MediaCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.util.PropertyFileLoader;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CHANNEL_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CHECKSUM_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.COURTHOUSE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.COURTROOM_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CREATED_DATE_TIME_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.END_DATE_TIME_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.FILE_TYPE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.HEARING_DATE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.MAX_CHANNELS_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.OBJECT_ID_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.OBJECT_TYPE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.PARENT_ID_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.START_DATE_TIME_KEY;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.GodClass"})
public class MediaArchiveRecordMapperImpl implements MediaArchiveRecordMapper {

    public static final String CASE_LIST_DELIMITER = "|";
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final CurrentTimeHelper currentTimeHelper;
    private Properties mediaRecordProperties;

    private DateTimeFormatter dateTimeFormatter;
    private DateTimeFormatter dateFormatter;
    
    @Override
    public MediaArchiveRecord mapToMediaArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                      String rawFilename) {
        dateTimeFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());
        dateFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateFormat());
        try {
            loadMediaProperties();
            MediaEntity media = externalObjectDirectory.getMedia();
            MediaCreateArchiveRecordOperation mediaCreateArchiveRecordOperation = createArchiveRecordOperation(externalObjectDirectory);
            UploadNewFileRecord uploadNewFileRecord = createUploadNewFileRecord(media, externalObjectDirectory.getId(), rawFilename);
            return createMediaArchiveRecord(mediaCreateArchiveRecordOperation, uploadNewFileRecord);
        } catch (IOException e) {
            log.error("Unable to read media property file {} - {}", armDataManagementConfiguration.getMediaRecordPropertiesFile(), e.getMessage());
        }
        return null;
    }

    private void loadMediaProperties() throws IOException {
        if (isNull(mediaRecordProperties) || mediaRecordProperties.isEmpty()) {
            mediaRecordProperties = PropertyFileLoader.loadPropertiesFromFile(armDataManagementConfiguration.getMediaRecordPropertiesFile());
        }
        if (mediaRecordProperties.isEmpty()) {
            log.warn("Failed to load property file {}", armDataManagementConfiguration.getMediaRecordPropertiesFile());
        }
    }

    private MediaArchiveRecord createMediaArchiveRecord(MediaCreateArchiveRecordOperation mediaCreateArchiveRecordOperation,
                                                        UploadNewFileRecord uploadNewFileRecord) {
        return MediaArchiveRecord.builder()
            .mediaCreateArchiveRecord(mediaCreateArchiveRecordOperation)
            .uploadNewFileRecord(uploadNewFileRecord)
            .build();
    }

    private MediaCreateArchiveRecordOperation createArchiveRecordOperation(ExternalObjectDirectoryEntity externalObjectDirectory) {
        return MediaCreateArchiveRecordOperation.builder()
            .relationId(String.valueOf(externalObjectDirectory.getId()))
            .recordMetadata(createArchiveRecordMetadata(externalObjectDirectory))
            .build();
    }

    @SuppressWarnings({"java:S3776", "PMD.CyclomaticComplexity", "PMD.CognitiveComplexity", "PMD.NPathComplexity"})
    private RecordMetadata createArchiveRecordMetadata(ExternalObjectDirectoryEntity externalObjectDirectory) {
        MediaEntity media = externalObjectDirectory.getMedia();
        OffsetDateTime retainUntilTs = media.getRetainUntilTs();

        RecordMetadata metadata = RecordMetadata.builder()
            .publisher(armDataManagementConfiguration.getPublisher())
            .recordClass(armDataManagementConfiguration.getMediaRecordClass())
            .recordDate(formatDateTime(currentTimeHelper.currentOffsetDateTime()))
            .eventDate(formatDateTime(nonNull(retainUntilTs)
                                          ? retainUntilTs.minusYears(armDataManagementConfiguration.getEventDateAdjustmentYears())
                                          : media.getCreatedDateTime()))
            .region(armDataManagementConfiguration.getRegion())
            .title(media.getMediaFile())
            .clientId(String.valueOf(externalObjectDirectory.getId()))
            .build();

        String courthouse = getCourthouse(media);
        String courtroom = getCourtroom(media);
        if (nonNull(courthouse) && nonNull(courtroom)) {
            metadata.setContributor(courthouse + " & " + courtroom);
        }

        if (nonNull(media.getRetConfReason())) {
            metadata.setRetentionConfidenceReason(media.getRetConfReason());
        }

        if (nonNull(media.getRetConfScore())) {
            metadata.setRetentionConfidenceScore(media.getRetConfScore().getId());
        }

        setMetadataProperties(metadata, media);
        return metadata;
    }

    private void setMetadataProperties(RecordMetadata metadata, MediaEntity mediaEntity) {
        for (String key : mediaRecordProperties.stringPropertyNames()) {
            String value = mapToString(mediaRecordProperties.getProperty(key), mediaEntity);
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
                    case BF_012_KEY -> metadata.setBf012(mapToInt(value, mediaEntity));
                    case BF_013_KEY -> metadata.setBf013(mapToInt(value, mediaEntity));
                    case BF_014_KEY -> metadata.setBf014(mapToInt(value, mediaEntity));
                    case BF_015_KEY -> metadata.setBf015(mapToInt(value, mediaEntity));
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

    @SuppressWarnings({"PMD.CyclomaticComplexity"})
    private String mapToString(String key, MediaEntity media) {
        return switch (key) {
            case OBJECT_TYPE_KEY -> ArchiveRecordType.MEDIA_ARCHIVE_TYPE.getArchiveTypeDescription();
            case CASE_NUMBERS_KEY -> getCaseNumbers(media);
            case FILE_TYPE_KEY -> media.getMediaFormat();
            case HEARING_DATE_KEY -> getHearingDate(media);
            case CHECKSUM_KEY -> media.getChecksum();
            case CREATED_DATE_TIME_KEY -> formatDateTime(media.getCreatedDateTime());
            case START_DATE_TIME_KEY -> formatDateTime(media.getStart());
            case END_DATE_TIME_KEY -> formatDateTime(media.getEnd());
            case COURTHOUSE_KEY -> getCourthouse(media);
            case COURTROOM_KEY -> getCourtroom(media);
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

    private String getHearingDate(MediaEntity media) {
        String hearingDate = null;
        if (CollectionUtils.isNotEmpty(media.getHearingList())) {
            hearingDate = OffsetDateTime.of(media.getHearingList().get(0).getHearingDate().atTime(0, 0, 0),
                                            ZoneOffset.UTC).format(dateTimeFormatter);
        }
        return hearingDate;
    }

    private String getCaseNumbers(MediaEntity media) {
        String cases = null;
        if (CollectionUtils.isNotEmpty(media.getHearingList())) {
            List<String> caseNumbers = media.getHearingList()
                .stream()
                .map(HearingEntity::getCourtCase)
                .map(CourtCaseEntity::getCaseNumber)
                .toList();
            cases = caseListToString(caseNumbers);
        }
        return cases;
    }

    private static String getCourthouse(MediaEntity media) {
        String courthouse = null;
        if (CollectionUtils.isNotEmpty(media.getHearingList()) && nonNull(media.getHearingList().getFirst().getCourtroom())) {
            courthouse = media.getHearingList().getFirst().getCourtroom().getCourthouse().getDisplayName();
        } else if (nonNull(media.getCourtroom()) && nonNull(media.getCourtroom().getCourthouse())) {
            courthouse = media.getCourtroom().getCourthouse().getDisplayName();
        }
        return courthouse;
    }

    private static String getCourtroom(MediaEntity media) {
        String courtroom = null;
        if (CollectionUtils.isNotEmpty(media.getHearingList()) && nonNull(media.getHearingList().get(0).getCourtroom())) {
            courtroom = media.getHearingList().get(0).getCourtroom().getName();
        } else if (nonNull(media.getCourtroom())) {
            courtroom = media.getCourtroom().getName();
        }
        return courtroom;
    }

    private Integer mapToInt(String key, MediaEntity media) {
        return switch (key) {
            case OBJECT_ID_KEY, PARENT_ID_KEY -> media.getId();
            case CHANNEL_KEY -> media.getChannel();
            case MAX_CHANNELS_KEY -> media.getTotalChannels();
            default -> null;
        };
    }

    private String caseListToString(List<String> caseNumberList) {
        return String.join(CASE_LIST_DELIMITER, caseNumberList);
    }

    private UploadNewFileRecord createUploadNewFileRecord(MediaEntity media, Integer relationId, String rawFilename) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(String.valueOf(relationId));
        uploadNewFileRecord.setFileMetadata(createUploadNewFileRecordMetadata(media, rawFilename));
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata(MediaEntity media, String rawFilename) {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher(armDataManagementConfiguration.getPublisher());
        uploadNewFileRecordMetadata.setDzFilename(rawFilename);
        uploadNewFileRecordMetadata.setFileTag(media.getMediaFormat());
        return uploadNewFileRecordMetadata;
    }

}
