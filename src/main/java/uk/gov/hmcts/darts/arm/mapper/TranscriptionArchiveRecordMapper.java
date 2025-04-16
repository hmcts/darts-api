package uk.gov.hmcts.darts.arm.mapper;

import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

@FunctionalInterface
public interface TranscriptionArchiveRecordMapper {
    TranscriptionArchiveRecord mapToTranscriptionArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                               String rawFilename);
}
