package uk.gov.hmcts.darts.arm.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.io.File;

@Component
@RequiredArgsConstructor
public class AnnotationArchiveRecordMapperImpl implements AnnotationArchiveRecordMapper {

    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    @Override
    public AnnotationArchiveRecord mapToAnnotationArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                String relationId, File archiveRecordFile) {
        //TODO fill in behaviour
        throw new NotImplementedException();
    }
}
