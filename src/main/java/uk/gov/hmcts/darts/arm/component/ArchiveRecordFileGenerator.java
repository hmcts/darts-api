package uk.gov.hmcts.darts.arm.component;

import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;

import java.io.File;
import java.util.List;

public interface ArchiveRecordFileGenerator {

    boolean generateArchiveRecord(ArchiveRecord archiveRecord, File archiveRecordFile, ArchiveRecordType archiveRecordType);

    String generateArchiveRecords(String archiveFileName, List<ArchiveRecord> archiveRecords);
}
