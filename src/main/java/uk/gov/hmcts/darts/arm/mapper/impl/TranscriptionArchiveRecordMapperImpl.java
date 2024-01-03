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

@Component
@RequiredArgsConstructor
public class TranscriptionArchiveRecordMapperImpl implements TranscriptionArchiveRecordMapper {

    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    @Override
    public TranscriptionArchiveRecord mapToTranscriptionArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, File archiveRecordFile) {
        TranscriptionDocumentEntity transcriptionDocument = externalObjectDirectory.getTranscriptionDocumentEntity();
        TranscriptionCreateArchiveRecordOperation transcriptionCreateArchiveRecordOperation = createArchiveRecordOperation(externalObjectDirectory,
                                                                                                                           externalObjectDirectory.getId());
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
        return UploadNewFileRecord.builder()
            .relationId(relationId.toString())
            .fileMetadata(createUploadNewFileRecordMetadata(transcriptionDocument))
            .build();
    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata(TranscriptionDocumentEntity transcriptionDocument) {
        return UploadNewFileRecordMetadata.builder()
            .publisher(armDataManagementConfiguration.getPublisher())
            .dzFilename(transcriptionDocument.getFileName())
            .fileTag(transcriptionDocument.getFileType())
            .build();
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
