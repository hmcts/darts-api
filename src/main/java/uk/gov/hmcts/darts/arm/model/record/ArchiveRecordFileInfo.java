package uk.gov.hmcts.darts.arm.model.record;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;

import java.io.File;

@Data
@Builder
public class ArchiveRecordFileInfo {

    private File archiveRecordFile;
    private boolean fileGenerationSuccessful;
    private ArchiveRecordType archiveRecordType;
}
