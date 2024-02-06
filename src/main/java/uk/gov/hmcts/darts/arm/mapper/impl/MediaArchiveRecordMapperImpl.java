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
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaArchiveRecordMapperImpl extends BaseArchiveRecordMapper implements MediaArchiveRecordMapper {

    public static final String CASE_LIST_DELIMITER = "|";
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final CurrentTimeHelper currentTimeHelper;
    private Properties mediaRecordProperties;

    DateTimeFormatter dateTimeFormatter;
    DateTimeFormatter dateFormatter;


    public MediaArchiveRecord mapToMediaArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, File archiveRecordFile) {
        try {
            dateTimeFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());
            dateFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateFormat());

            mediaRecordProperties = PropertyFileLoader.loadPropertiesFromFile(armDataManagementConfiguration.getMediaRecordPropertiesFile());
            MediaEntity media = externalObjectDirectory.getMedia();
            MediaCreateArchiveRecordOperation mediaCreateArchiveRecordOperation = createArchiveRecordOperation(externalObjectDirectory);
            UploadNewFileRecord uploadNewFileRecord = createUploadNewFileRecord(media, externalObjectDirectory.getId());
            return createMediaArchiveRecord(mediaCreateArchiveRecordOperation, uploadNewFileRecord);
        } catch (IOException e) {
            log.error("Unable to read media property file {} - {}", armDataManagementConfiguration.getMediaRecordPropertiesFile(), e.getMessage());
        }
        return null;
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

    private RecordMetadata createArchiveRecordMetadata(ExternalObjectDirectoryEntity externalObjectDirectory) {
        MediaEntity media = externalObjectDirectory.getMedia();

        RecordMetadata metadata = RecordMetadata.builder()
            .publisher(armDataManagementConfiguration.getPublisher())
            .recordClass(armDataManagementConfiguration.getMediaRecordClass())
            .recordDate(currentTimeHelper.currentOffsetDateTime().format(dateTimeFormatter))
            .region(armDataManagementConfiguration.getRegion())
            .build();

        if (mediaRecordProperties.containsKey(BF_001_KEY)) {
            metadata.setBf001(mapToString(mediaRecordProperties.getProperty(BF_001_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_002_KEY)) {
            metadata.setBf002(mapToString(mediaRecordProperties.getProperty(BF_002_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_003_KEY)) {
            metadata.setBf003(mapToString(mediaRecordProperties.getProperty(BF_003_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_004_KEY)) {
            metadata.setBf004(mapToString(mediaRecordProperties.getProperty(BF_004_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_005_KEY)) {
            metadata.setBf005(mapToString(mediaRecordProperties.getProperty(BF_005_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_006_KEY)) {
            metadata.setBf006(mapToString(mediaRecordProperties.getProperty(BF_006_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_007_KEY)) {
            metadata.setBf007(mapToString(mediaRecordProperties.getProperty(BF_007_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_008_KEY)) {
            metadata.setBf008(mapToString(mediaRecordProperties.getProperty(BF_008_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_009_KEY)) {
            metadata.setBf009(mapToString(mediaRecordProperties.getProperty(BF_009_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_010_KEY)) {
            metadata.setBf010(mapToString(mediaRecordProperties.getProperty(BF_010_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_011_KEY)) {
            metadata.setBf011(mapToString(mediaRecordProperties.getProperty(BF_011_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_012_KEY)) {
            metadata.setBf012(mapToInt(mediaRecordProperties.getProperty(BF_012_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_013_KEY)) {
            metadata.setBf013(mapToInt(mediaRecordProperties.getProperty(BF_013_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_014_KEY)) {
            metadata.setBf014(mapToInt(mediaRecordProperties.getProperty(BF_014_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_015_KEY)) {
            metadata.setBf015(mapToInt(mediaRecordProperties.getProperty(BF_015_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_016_KEY)) {
            metadata.setBf016(mapToString(mediaRecordProperties.getProperty(BF_016_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_017_KEY)) {
            metadata.setBf017(mapToString(mediaRecordProperties.getProperty(BF_017_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_018_KEY)) {
            metadata.setBf018(mapToString(mediaRecordProperties.getProperty(BF_018_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_019_KEY)) {
            metadata.setBf019(mapToString(mediaRecordProperties.getProperty(BF_019_KEY), media));
        }
        if (mediaRecordProperties.containsKey(BF_020_KEY)) {
            metadata.setBf020(mapToString(mediaRecordProperties.getProperty(BF_020_KEY), media));
        }
        return metadata;
    }

    private String mapToString(String key, MediaEntity media) {
        return switch (key) {
            case "OBJECT_TYPE" -> ArchiveRecordType.MEDIA_ARCHIVE_TYPE.getArchiveTypeDescription();
            case "CASE_NUMBERS" -> {
                String cases = null;
                if (nonNull(media.getCaseNumberList())) {
                    cases = caseListToString(media.getCaseNumberList());
                }
                yield cases;
            }
            case "FILE_TYPE" -> media.getMediaFormat();
            case "HEARING_DATE" -> {
                String hearingDate = null;
                if (CollectionUtils.isNotEmpty(media.getHearingList())) {
                    hearingDate = media.getHearingList().get(0).getHearingDate().format(dateFormatter);
                }
                yield hearingDate;
            }
            case "CHECKSUM" -> media.getChecksum();
//            case "TRANSCRIPT_REQUEST" -> ;
//            case "TRANSCRIPT_TYPE" -> ;
//            case "TRANSCRIPT_URGENCY" -> ;
//            case "COMMENTS" -> ;
//            case "PLACEHOLDER_DATE" -> ;
            case "CREATED_DATE_TIME" -> {
                String createdDateTime = null;
                if (nonNull(media.getCreatedDateTime())) {
                    createdDateTime = media.getCreatedDateTime().format(dateTimeFormatter);
                }
                yield createdDateTime;
            }

//            case "UPLOADED_BY" -> ;
            case "START_DATE_TIME" -> media.getStart().format(dateTimeFormatter);
            case "END_DATE_TIME" -> media.getEnd().format(dateTimeFormatter);
            case "COURTHOUSE" -> media.getCourtroom().getCourthouse().getCourthouseName();
            case "COURTROOM" -> media.getCourtroom().getName();
            default -> null;
        };
    }

    private Integer mapToInt(String key, MediaEntity media) {
        return switch (key) {
            case "OBJECT_ID" -> media.getId();
            case "PARENT_ID" -> media.getId();
            case "CHANNEL" -> media.getChannel();
            case "MAX_CHANNELS" -> media.getTotalChannels();
            default -> null;
        };
    }


    private String caseListToString(List<String> caseIdList) {
        return String.join(CASE_LIST_DELIMITER, caseIdList);
    }

    private UploadNewFileRecord createUploadNewFileRecord(MediaEntity media, Integer relationId) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(relationId.toString());
        uploadNewFileRecord.setFileMetadata(createUploadNewFileRecordMetadata(media));
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata(MediaEntity media) {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher(armDataManagementConfiguration.getPublisher());
        uploadNewFileRecordMetadata.setDzFilename(media.getMediaFile());
        uploadNewFileRecordMetadata.setFileTag(media.getMediaFormat());
        return uploadNewFileRecordMetadata;
    }

}
