package uk.gov.hmcts.darts.arm.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.TranscriptionCreateArchiveRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.TranscriptionCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;

import java.io.File;

import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.UPLOAD_NEW_FILE;

@Component
@RequiredArgsConstructor
public class TranscriptionArchiveRecordMapperImpl implements TranscriptionArchiveRecordMapper {

    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    @Override
    public TranscriptionArchiveRecord mapToTranscriptionArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, File archiveRecordFile) {
        TranscriptionDocumentEntity transcriptionDocument = externalObjectDirectory.getTranscriptionDocumentEntity();
        TranscriptionCreateArchiveRecordOperation transcriptionCreateArchiveRecordOperation = createArchiveRecordOperation(
              externalObjectDirectory,
              externalObjectDirectory.getId()
        );
        UploadNewFileRecord uploadNewFileRecord = createUploadNewFileRecord(transcriptionDocument, externalObjectDirectory.getId());
        return createTranscriptionArchiveRecord(transcriptionCreateArchiveRecordOperation, uploadNewFileRecord);
    }

    private TranscriptionArchiveRecord createTranscriptionArchiveRecord(TranscriptionCreateArchiveRecordOperation transcriptionCreateArchiveRecordOperation,
                                                                        UploadNewFileRecord uploadNewFileRecord) {
        return TranscriptionArchiveRecord.builder()
              .transcriptionCreateArchiveRecordOperation(transcriptionCreateArchiveRecordOperation)
              .uploadNewFileRecord(uploadNewFileRecord)
              .build();
    }

    private UploadNewFileRecord createUploadNewFileRecord(TranscriptionDocumentEntity transcriptionDocument, Integer relationId) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(relationId.toString());
        uploadNewFileRecord.setFileMetadata(createUploadNewFileRecordMetadata(transcriptionDocument));
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata(TranscriptionDocumentEntity transcriptionDocument) {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher(armDataManagementConfiguration.getPublisher());
        uploadNewFileRecordMetadata.setDzFilename(transcriptionDocument.getFileName());
        uploadNewFileRecordMetadata.setFileTag(transcriptionDocument.getFileType());
        return uploadNewFileRecordMetadata;
    }

    private TranscriptionCreateArchiveRecordOperation createArchiveRecordOperation(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                                   Integer relationId) {
        return TranscriptionCreateArchiveRecordOperation.builder()
              .relationId(relationId.toString())
              .recordMetadata(createArchiveRecordMetadata(externalObjectDirectory))
              .build();
    }

    private TranscriptionCreateArchiveRecordMetadata createArchiveRecordMetadata(ExternalObjectDirectoryEntity externalObjectDirectory) {
        return TranscriptionCreateArchiveRecordMetadata.builder()
              .publisher(armDataManagementConfiguration.getPublisher())
              .build();
    }
}
