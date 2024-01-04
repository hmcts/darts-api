package uk.gov.hmcts.darts.arm.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.MediaCreateArchiveRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.MediaCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Objects.nonNull;

@Component
@RequiredArgsConstructor
public class MediaArchiveRecordMapperImpl implements MediaArchiveRecordMapper {

    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    public MediaArchiveRecord mapToMediaArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, File archiveRecordFile) {
        MediaEntity media = externalObjectDirectory.getMedia();
        MediaCreateArchiveRecordOperation mediaCreateArchiveRecordOperation = createArchiveRecordOperation(
            externalObjectDirectory,
            externalObjectDirectory.getId()
        );
        UploadNewFileRecord uploadNewFileRecord = createUploadNewFileRecord(media, externalObjectDirectory.getId());
        return createMediaArchiveRecord(mediaCreateArchiveRecordOperation, uploadNewFileRecord);
    }

    private MediaArchiveRecord createMediaArchiveRecord(MediaCreateArchiveRecordOperation mediaCreateArchiveRecordOperation,
                                                        UploadNewFileRecord uploadNewFileRecord) {
        return MediaArchiveRecord.builder()
            .mediaCreateArchiveRecord(mediaCreateArchiveRecordOperation)
            .uploadNewFileRecord(uploadNewFileRecord)
            .build();
    }

    private MediaCreateArchiveRecordOperation createArchiveRecordOperation(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                           Integer relationId) {
        return MediaCreateArchiveRecordOperation.builder()
            .relationId(relationId.toString())
            .recordMetadata(createArchiveRecordMetadata(externalObjectDirectory))
            .build();
    }

    private MediaCreateArchiveRecordMetadata createArchiveRecordMetadata(ExternalObjectDirectoryEntity externalObjectDirectory) {
        MediaEntity media = externalObjectDirectory.getMedia();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());

        MediaCreateArchiveRecordMetadata metadata = MediaCreateArchiveRecordMetadata.builder()
            .publisher(armDataManagementConfiguration.getPublisher())
            .recordClass(armDataManagementConfiguration.getMediaRecordClass())
            .recordDate(OffsetDateTime.now().format(formatter))
            .region(armDataManagementConfiguration.getRegion())
            .id(media.getId().toString())
            .type(ArchiveRecordType.MEDIA_ARCHIVE_TYPE.getArchiveTypeDescription())
            .channel(media.getChannel().toString())
            .maxChannels(media.getTotalChannels().toString())
            .courthouse(media.getCourtroom().getCourthouse().getCourthouseName())
            .courtroom(media.getCourtroom().getName())
            .fileName(media.getMediaFile())
            .fileFormat(media.getMediaFormat())
            .startDateTime(media.getStart().format(formatter))
            .endDateTime(media.getEnd().format(formatter))
            .build();

        if (nonNull(media.getCreatedDateTime())) {
            metadata.setCreatedDateTime(media.getCreatedDateTime().format(formatter));
        }
        if (nonNull(media.getCaseNumberList())) {
            metadata.setCaseNumbers(caseListToString(media.getCaseNumberList()));
        }
        return metadata;
    }

    private String caseListToString(List<String> caseIdList) {
        return String.join("|", caseIdList);
    }

    private UploadNewFileRecord createUploadNewFileRecord(MediaEntity media, Integer relationId) {
        return UploadNewFileRecord.builder()
            .relationId(relationId.toString())
            .fileMetadata(createUploadNewFileRecordMetadata(media))
            .build();
    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata(MediaEntity media) {
        return UploadNewFileRecordMetadata.builder()
            .publisher(armDataManagementConfiguration.getPublisher())
            .dzFilename(media.getMediaFile()) //"<EOD>_<MEDID>_<ATTEMPT>.mp2"
            .fileTag(media.getMediaFormat())
            .build();
    }
}
