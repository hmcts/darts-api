package uk.gov.hmcts.darts.arm.model.record;

import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.model.ArchiveRecordOperation;
import uk.gov.hmcts.darts.arm.model.record.operation.CaseCreateArchiveRecordOperation;

@Value
@Builder
public class CaseArchiveRecord implements ArchiveRecord {
    private CaseCreateArchiveRecordOperation caseCreateArchiveRecordOperation;
    private UploadNewFileRecord uploadNewFileRecord;

    @Override
    public ArchiveRecordOperation getArchiveRecordOperation() {
        return caseCreateArchiveRecordOperation;
    }

    @Override
    public UploadNewFileRecord getUploadNewFileRecord() {
        return uploadNewFileRecord;
    }
}
