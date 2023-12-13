package uk.gov.hmcts.darts.arm.mapper.impl;

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

public class MediaArchiveRecordMapperImpl implements MediaArchiveRecordMapper {

    private ArmDataManagementConfiguration armDataManagementConfiguration;

    public MediaArchiveRecord mapToMediaArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, String relationId, File archiveRecordFile) {
        MediaEntity media = externalObjectDirectory.getMedia();
        MediaCreateArchiveRecordOperation mediaCreateArchiveRecordOperation = createArchiveRecordOperation(externalObjectDirectory, relationId);
        UploadNewFileRecord uploadNewFileRecord = createUploadNewFileRecord(media, relationId);
        return createMediaArchiveRecord(mediaCreateArchiveRecordOperation, uploadNewFileRecord);
    }

    private MediaArchiveRecord createMediaArchiveRecord(MediaCreateArchiveRecordOperation mediaCreateArchiveRecordOperation,
                                                        UploadNewFileRecord uploadNewFileRecord) {
        return MediaArchiveRecord.builder()
            .mediaCreateArchiveRecord(mediaCreateArchiveRecordOperation)
            .uploadNewFileRecord(uploadNewFileRecord)
            .build();
    }

    private MediaCreateArchiveRecordOperation createArchiveRecordOperation(ExternalObjectDirectoryEntity externalObjectDirectory, String relationId) {
        return MediaCreateArchiveRecordOperation.builder()
            .relationId(relationId)
            .recordMetadata(createArchiveRecordMetadata(externalObjectDirectory))
            .build();
    }

    private MediaCreateArchiveRecordMetadata createArchiveRecordMetadata(ExternalObjectDirectoryEntity externalObjectDirectory) {
        MediaEntity media = externalObjectDirectory.getMedia();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());

        return MediaCreateArchiveRecordMetadata.builder()
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
            .mediaFile(media.getMediaFile())
            .mediaFormat(media.getMediaFormat())
            .startDateTime(media.getStart().format(formatter))
            .endDateTime(media.getEnd().format(formatter))
            .createdDateTime(media.getCreatedDateTime().format(formatter))
            .caseNumbers(caseListToString(media.getCaseIdList()))
            .build();
    }

    private String caseListToString(List<String> caseIdList) {
        return String.join("|", caseIdList);
    }


    private UploadNewFileRecord createUploadNewFileRecord(MediaEntity media, String relationId) {
        UploadNewFileRecord uploadNewFileRecord = UploadNewFileRecord.builder()
            .relationId(relationId)
            .fileMetadata(createUploadNewFileRecordMetadata(media))
            .build();
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata(MediaEntity media) {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = UploadNewFileRecordMetadata.builder()
            .publisher(armDataManagementConfiguration.getPublisher())
            .dzFilename(media.getMediaFile()) //"<EOD>_<MEDID>_<ATTEMPT>.mp2"
            .fileTag(media.getMediaFormat())
            .build();
        return uploadNewFileRecordMetadata;
    }
}
