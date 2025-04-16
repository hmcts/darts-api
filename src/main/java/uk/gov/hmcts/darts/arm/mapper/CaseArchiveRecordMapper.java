package uk.gov.hmcts.darts.arm.mapper;

import uk.gov.hmcts.darts.arm.model.record.CaseArchiveRecord;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

@FunctionalInterface
public interface CaseArchiveRecordMapper {
    CaseArchiveRecord mapToCaseArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory,
                                             String rawFilename);
}
