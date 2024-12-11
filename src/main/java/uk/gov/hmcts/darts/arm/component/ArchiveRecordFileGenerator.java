package uk.gov.hmcts.darts.arm.component;

import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;

import java.io.File;
import java.util.List;

public interface ArchiveRecordFileGenerator {
    boolean generateArchiveRecord(ArchiveRecord archiveRecord, File archiveRecordFile, ArchiveRecordType archiveRecordType);

    String generateArchiveRecords(String archvieFileName, List<ArchiveRecord> archiveRecords);

    @Deprecated(since = "11/12/2024")
    //Perfered method is to write to a string and upload to blob storage instead of using file operations
    void generateArchiveRecords(List<ArchiveRecord> archiveRecords, File archiveRecordsFile);
}
