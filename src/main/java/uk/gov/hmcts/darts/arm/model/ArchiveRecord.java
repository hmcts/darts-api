package uk.gov.hmcts.darts.arm.model;

import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;

public interface ArchiveRecord {
    public ArchiveRecordOperation getArchiveRecordOperation();

    public UploadNewFileRecord getUploadNewFileRecord();

}
