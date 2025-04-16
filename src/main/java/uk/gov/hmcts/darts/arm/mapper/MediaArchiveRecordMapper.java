package uk.gov.hmcts.darts.arm.mapper;

import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

@FunctionalInterface
public interface MediaArchiveRecordMapper {
    MediaArchiveRecord mapToMediaArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory,
                                               String rawFilename);
}
