package uk.gov.hmcts.darts.arm.model;

import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;

public interface ArchiveRecord {

    ArchiveRecordOperation getArchiveRecordOperation();

    UploadNewFileRecord getUploadNewFileRecord();

}
