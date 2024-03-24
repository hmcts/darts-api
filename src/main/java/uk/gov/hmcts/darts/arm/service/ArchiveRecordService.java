package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;

public interface ArchiveRecordService {

    ArchiveRecordFileInfo generateArchiveRecord(Integer externalObjectDirectoryId, String rawFilename);

    ArchiveRecord generateArchiveRecordInfo(Integer externalObjectDirectoryId, String rawFilename);
}
