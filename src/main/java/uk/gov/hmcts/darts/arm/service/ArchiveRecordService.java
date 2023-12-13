package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

public interface ArchiveRecordService {

    ArchiveRecordFileInfo generateArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, Integer archiveRecordAttempt);
}
