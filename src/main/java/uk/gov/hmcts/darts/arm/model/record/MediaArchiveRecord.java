package uk.gov.hmcts.darts.arm.model.record;

import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.model.ArchiveRecordOperation;
import uk.gov.hmcts.darts.arm.model.record.operation.MediaCreateArchiveRecordOperation;


@Value
@Builder
public class MediaArchiveRecord implements ArchiveRecord {

    private MediaCreateArchiveRecordOperation mediaCreateArchiveRecord;
    private UploadNewFileRecord uploadNewFileRecord;

    @Override
    public ArchiveRecordOperation getArchiveRecordOperation() {
        return mediaCreateArchiveRecord;
    }

    @Override
    public UploadNewFileRecord getUploadNewFileRecord() {
        return uploadNewFileRecord;
    }
}
