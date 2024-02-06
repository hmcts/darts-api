package uk.gov.hmcts.darts.arm.model.record;

import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.model.ArchiveRecordOperation;
import uk.gov.hmcts.darts.arm.model.record.operation.AnnotationCreateArchiveRecordOperation;

@Value
@Builder
public class AnnotationArchiveRecord implements ArchiveRecord {

    private AnnotationCreateArchiveRecordOperation annotationCreateArchiveRecordOperation;
    private UploadNewFileRecord uploadNewFileRecord;

    @Override
    public ArchiveRecordOperation getArchiveRecordOperation() {
        return annotationCreateArchiveRecordOperation;
    }

    @Override
    public UploadNewFileRecord getUploadNewFileRecord() {
        return uploadNewFileRecord;
    }
}
