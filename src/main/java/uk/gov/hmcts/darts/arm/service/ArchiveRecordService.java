package uk.gov.hmcts.darts.arm.service;

import java.io.File;

public interface ArchiveRecordService {

    File generateArchiveRecord(Integer externalObjectDirectoryId, String relationId, String archiveRecordFilename);
}
