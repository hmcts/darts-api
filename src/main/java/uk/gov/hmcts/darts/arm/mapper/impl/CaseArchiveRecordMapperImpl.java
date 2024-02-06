package uk.gov.hmcts.darts.arm.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.mapper.CaseArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.CaseArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.CaseCreateArchiveRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.CaseCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.io.File;

import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.UPLOAD_NEW_FILE;

@Component
@RequiredArgsConstructor
public class CaseArchiveRecordMapperImpl implements CaseArchiveRecordMapper {

    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    @Override
    public CaseArchiveRecord mapToCaseArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, File archiveRecordFile) {
        CaseDocumentEntity caseDocument = externalObjectDirectory.getCaseDocument();
        CaseCreateArchiveRecordOperation caseCreateArchiveRecordOperation = createArchiveRecordOperation(
            externalObjectDirectory,
            externalObjectDirectory.getId()
        );
        UploadNewFileRecord uploadNewFileRecord = createUploadNewFileRecord(caseDocument, externalObjectDirectory.getId());
        return createCaseArchiveRecord(caseCreateArchiveRecordOperation, uploadNewFileRecord);
    }

    private CaseArchiveRecord createCaseArchiveRecord(CaseCreateArchiveRecordOperation caseCreateArchiveRecordOperation,
                                                      UploadNewFileRecord uploadNewFileRecord) {
        return CaseArchiveRecord.builder()
            .caseCreateArchiveRecordOperation(caseCreateArchiveRecordOperation)
            .uploadNewFileRecord(uploadNewFileRecord)
            .build();
    }

    private UploadNewFileRecord createUploadNewFileRecord(CaseDocumentEntity caseDocument, Integer relationId) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(relationId.toString());
        uploadNewFileRecord.setFileMetadata(createUploadNewFileRecordMetadata(caseDocument));
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata(CaseDocumentEntity caseDocument) {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher(armDataManagementConfiguration.getPublisher());
        uploadNewFileRecordMetadata.setDzFilename(caseDocument.getFileName());
        uploadNewFileRecordMetadata.setFileTag(caseDocument.getFileType());
        return uploadNewFileRecordMetadata;
    }

    private CaseCreateArchiveRecordOperation createArchiveRecordOperation(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                          Integer relationId) {
        return CaseCreateArchiveRecordOperation.builder()
            .relationId(relationId.toString())
            .recordMetadata(createArchiveRecordMetadata(externalObjectDirectory))
            .build();
    }

    private CaseCreateArchiveRecordMetadata createArchiveRecordMetadata(ExternalObjectDirectoryEntity externalObjectDirectory) {
        return CaseCreateArchiveRecordMetadata.builder()
            .publisher(armDataManagementConfiguration.getPublisher())
            .build();
    }
}
