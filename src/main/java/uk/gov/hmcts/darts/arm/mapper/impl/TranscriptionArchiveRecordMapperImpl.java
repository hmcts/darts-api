package uk.gov.hmcts.darts.arm.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.io.File;

@Component
@RequiredArgsConstructor
public class TranscriptionArchiveRecordMapperImpl implements TranscriptionArchiveRecordMapper {

    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    @Override
    public TranscriptionArchiveRecord mapToTranscriptionArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, File archiveRecordFile) {
        //TODO fill in behaviour
        throw new NotImplementedException();
    }
}
