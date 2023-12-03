package uk.gov.hmcts.darts.archiverecords.mapper;

import uk.gov.hmcts.darts.archiverecords.model.metadata.MediaCreateArchiveRecordMetadata;
import uk.gov.hmcts.darts.archiverecords.model.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.archiverecords.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.archiverecords.model.record.MediaCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.archiverecords.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

public class MediaArchiveRecordMapper {

    private MediaArchiveRecord createMediaArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory) {
        MediaArchiveRecord mediaArchiveRecord = MediaArchiveRecord.builder().build();
        return mediaArchiveRecord;
    }

    private MediaArchiveRecord createMediaArchiveRecord(String relationId) {
        return MediaArchiveRecord.builder()
            .mediaCreateArchiveRecord(createArchiveRecord(relationId))
            .uploadNewFileRecord(createUploadNewFileRecord(relationId))
            .build();
    }

    private MediaCreateArchiveRecordOperation createArchiveRecord(String relationId) {
        return MediaCreateArchiveRecordOperation.builder()
            .relationId(relationId)
            .recordMetadata(createArchiveRecordMetadata())
            .build();
    }

    private MediaCreateArchiveRecordMetadata createArchiveRecordMetadata() {
        return MediaCreateArchiveRecordMetadata.builder()
            .publisher("DARTS")
            .recordClass("DARTSMedia")
            .recordDate("2023-07-19T11:39:30Z")
            .region("GBR")
            .id("12345")
            .type("Media")
            .channel("1")
            .maxChannels("4")
            .courthouse("Swansea")
            .courtroom("1234")
            .mediaFile("media_filename")
            .mediaFormat("mp2")
            .startDateTime("2023-07-18T11:39:30Z")
            .endDateTime("2023-07-18T12:39:30Z")
            .createdDateTime("2023-07-14T12:39:30Z")
            .caseNumbers("Case_1|Case_2|Case_3")
            .build();
    }


    private UploadNewFileRecord createUploadNewFileRecord(String relationId) {
        UploadNewFileRecord uploadNewFileRecord = UploadNewFileRecord.builder()
            .relationId(relationId)
            .fileMetadata(createUploadNewFileRecordMetadata())
            .build();
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata() {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = UploadNewFileRecordMetadata.builder()
            .publisher("DARTS")
            .dzFilename("<EOD>_<MEDID>_<ATTEMPT>.mp2")
            .fileTag("mp2")
            .build();
        return uploadNewFileRecordMetadata;
    }
}
