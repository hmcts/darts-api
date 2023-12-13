package uk.gov.hmcts.darts.arm.mapper.impl;

import org.apache.commons.lang3.NotImplementedException;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.io.File;

public class AnnotationArchiveRecordMapperIImpl implements AnnotationArchiveRecordMapper {

    @Override
    public AnnotationArchiveRecord mapToAnnotationArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, String relationId, File archiveRecordFile) {
        //TODO fill in behaviour
        throw new NotImplementedException();
    }
}
