package uk.gov.hmcts.darts.archiverecordsmanagement.model.impl;

import lombok.Data;


@Data
public class MediaArchiveRecord {
    private CreateArchiveRecord createArchiveRecord;
    private UploadNewFileRecord uploadNewFileRecord;
}
