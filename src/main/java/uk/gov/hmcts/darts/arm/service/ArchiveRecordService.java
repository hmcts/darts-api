package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;

import java.util.Map;

public interface ArchiveRecordService {

    Map<String, ArchiveRecordFileInfo> generateArchiveRecord(Integer externalObjectDirectoryId, Integer archiveRecordAttempt);
}
