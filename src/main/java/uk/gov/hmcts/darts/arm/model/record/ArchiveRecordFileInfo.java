package uk.gov.hmcts.darts.arm.model.record;

import lombok.Builder;
import lombok.Data;

import java.io.File;

@Data
@Builder
public class ArchiveRecordFileInfo {
    private File archiveRecordFile;
    private boolean fileGenerationSuccessful;
}
