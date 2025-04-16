package uk.gov.hmcts.darts.arm.mapper;

import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

@FunctionalInterface
public interface AnnotationArchiveRecordMapper {
    AnnotationArchiveRecord mapToAnnotationArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                         String rawFilename);
}
