package uk.gov.hmcts.darts.arm.enums;

import lombok.Getter;

@Getter
public enum ArchiveRecordType {
    MEDIA_ARCHIVE_TYPE("Media"),
    TRANSCRIPTION_ARCHIVE_TYPE("Transcription"),
    ANNOTATION_ARCHIVE_TYPE("Annotation");

    private String archiveTypeDescription;

    ArchiveRecordType(String archiveTypeDescription) {
        this.archiveTypeDescription = archiveTypeDescription;
    }
}
