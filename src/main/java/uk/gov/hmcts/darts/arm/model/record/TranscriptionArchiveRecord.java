package uk.gov.hmcts.darts.arm.model.record;

import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.model.ArchiveRecordOperation;
import uk.gov.hmcts.darts.arm.model.record.operation.TranscriptionCreateArchiveRecordOperation;

@Value
@Builder
public class TranscriptionArchiveRecord implements ArchiveRecord {

    private TranscriptionCreateArchiveRecordOperation transcriptionCreateArchiveRecordOperation;
    private UploadNewFileRecord uploadNewFileRecord;

    @Override
    public ArchiveRecordOperation getArchiveRecordOperation() {
        return transcriptionCreateArchiveRecordOperation;
    }

    @Override
    public UploadNewFileRecord getUploadNewFileRecord() {
        return uploadNewFileRecord;
    }
}
