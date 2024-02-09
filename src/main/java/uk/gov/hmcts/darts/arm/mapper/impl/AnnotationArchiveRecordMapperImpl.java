package uk.gov.hmcts.darts.arm.mapper.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.AnnotationCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.util.PropertyFileLoader;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.UPLOAD_NEW_FILE;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnnotationArchiveRecordMapperImpl implements AnnotationArchiveRecordMapper {

    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final CurrentTimeHelper currentTimeHelper;
    private Properties annotationRecordProperties;

    private DateTimeFormatter dateTimeFormatter;
    private DateTimeFormatter dateFormatter;


    @Override
    public AnnotationArchiveRecord mapToAnnotationArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, File archiveRecordFile) {
        dateTimeFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());
        dateFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateFormat());

        try {
            loadAnnotationProperties();
            AnnotationDocumentEntity annotationDocument = externalObjectDirectory.getAnnotationDocumentEntity();
            AnnotationCreateArchiveRecordOperation annotationCreateArchiveRecordOperation = createArchiveRecordOperation(externalObjectDirectory);
            UploadNewFileRecord uploadNewFileRecord = createUploadNewFileRecord(annotationDocument, externalObjectDirectory.getId());
            return createAnnotationArchiveRecord(annotationCreateArchiveRecordOperation, uploadNewFileRecord);
        } catch (IOException e) {
            log.error(
                    "Unable to read annotation property file {} - {}",
                    armDataManagementConfiguration.getAnnotationRecordPropertiesFile(),
                    e.getMessage());
        }
        return null;
    }

    private void loadAnnotationProperties() throws IOException {
        if (isNull(annotationRecordProperties) || annotationRecordProperties.isEmpty()) {
            annotationRecordProperties = PropertyFileLoader.loadPropertiesFromFile(armDataManagementConfiguration.getAnnotationRecordPropertiesFile());
        }
    }

    private AnnotationArchiveRecord createAnnotationArchiveRecord(AnnotationCreateArchiveRecordOperation annotationCreateArchiveRecordOperation,
                                                                  UploadNewFileRecord uploadNewFileRecord) {
        return AnnotationArchiveRecord.builder()
                .annotationCreateArchiveRecordOperation(annotationCreateArchiveRecordOperation)
                .uploadNewFileRecord(uploadNewFileRecord)
                .build();
    }

    private UploadNewFileRecord createUploadNewFileRecord(AnnotationDocumentEntity annotationDocument, Integer relationId) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(relationId.toString());
        uploadNewFileRecord.setFileMetadata(createUploadNewFileRecordMetadata(annotationDocument));
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata(AnnotationDocumentEntity annotationDocument) {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher(armDataManagementConfiguration.getPublisher());
        uploadNewFileRecordMetadata.setDzFilename(annotationDocument.getFileName());
        uploadNewFileRecordMetadata.setFileTag(annotationDocument.getFileType());
        return uploadNewFileRecordMetadata;
    }

    private AnnotationCreateArchiveRecordOperation createArchiveRecordOperation(ExternalObjectDirectoryEntity externalObjectDirectory) {
        return AnnotationCreateArchiveRecordOperation.builder()
                .relationId(String.valueOf(externalObjectDirectory.getId()))
                .recordMetadata(createArchiveRecordMetadata(externalObjectDirectory))
                .build();
    }

    private RecordMetadata createArchiveRecordMetadata(ExternalObjectDirectoryEntity externalObjectDirectory) {
        AnnotationDocumentEntity annotationDocument = externalObjectDirectory.getAnnotationDocumentEntity();
        return RecordMetadata.builder()
                .publisher(armDataManagementConfiguration.getPublisher())
                .recordClass(armDataManagementConfiguration.getMediaRecordClass())
                .recordDate(currentTimeHelper.currentOffsetDateTime().format(dateTimeFormatter))
                .region(armDataManagementConfiguration.getRegion())
                .title(annotationDocument.getFileName())
                .clientId(String.valueOf(externalObjectDirectory.getId()))
                .build();
    }
}
