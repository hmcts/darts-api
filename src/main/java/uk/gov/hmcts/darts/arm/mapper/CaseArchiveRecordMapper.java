package uk.gov.hmcts.darts.arm.mapper;

import uk.gov.hmcts.darts.arm.model.record.CaseArchiveRecord;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.io.File;

public interface CaseArchiveRecordMapper {
    CaseArchiveRecord mapToCaseArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory,
                                             File archiveRecordFile,
                                             String rawFilename);
}
