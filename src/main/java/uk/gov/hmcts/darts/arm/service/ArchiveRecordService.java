package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.arm.model.ArchiveRecord;

public interface ArchiveRecordService {
    ArchiveRecord generateArchiveRecordInfo(Integer externalObjectDirectoryId, String rawFilename);
}
