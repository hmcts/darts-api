package uk.gov.hmcts.darts.arm.mapper.impl;

import org.apache.commons.lang3.NotImplementedException;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.io.File;

public class TranscriptionArchiveRecordMapperImpl implements TranscriptionArchiveRecordMapper {

    @Override
    public TranscriptionArchiveRecord mapToTranscriptionArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                      String relationId, File archiveRecordFile) {
        //TODO fill in behaviour
        throw new NotImplementedException();
    }
}
