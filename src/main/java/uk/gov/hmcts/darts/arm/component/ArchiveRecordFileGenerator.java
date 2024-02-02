package uk.gov.hmcts.darts.arm.component;

import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;

import java.io.File;

public interface ArchiveRecordFileGenerator {
    boolean generateArchiveRecord(ArchiveRecord archiveRecord, File archiveRecordFile, ArchiveRecordType archiveRecordType);

    boolean generateArchiveRecord(String archiveRecordContents, File archiveRecordFile, ArchiveRecordType archiveRecordType);
}
