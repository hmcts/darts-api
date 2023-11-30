package uk.gov.hmcts.darts.archiverecordsmanagement.model.record;

import lombok.Builder;
import lombok.Value;


@Value
@Builder
public class MediaArchiveRecord {
    private CreateArchiveRecord createArchiveRecord;
    private UploadNewFileRecord uploadNewFileRecord;
}
