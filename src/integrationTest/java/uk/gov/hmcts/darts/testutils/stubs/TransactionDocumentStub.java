package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;

@Component
@RequiredArgsConstructor
@Deprecated
public class TransactionDocumentStub {

    @Autowired
    private TranscriptionDocumentRepository transcriptionDocumentRepository;

    public TranscriptionDocumentEntity createTranscriptionDocument(String fileName,
                                                                   Integer filebytes, String fileType,
                                                                   boolean hidden, TranscriptionEntity entity) {
        TranscriptionDocumentEntity documentEntity = new TranscriptionDocumentEntity();
        documentEntity.setTranscription(entity);
        documentEntity.setHidden(hidden);
        documentEntity.setFileType(fileType);
        documentEntity.setFileName(fileName);
        documentEntity.setFileSize(filebytes);
        documentEntity.setUploadedBy(entity.getCreatedBy());
        documentEntity.setUploadedDateTime(entity.getCreatedDateTime());
        documentEntity.setLastModifiedBy(entity.getCreatedBy());

        return transcriptionDocumentRepository.saveAndFlush(documentEntity);
    }
}