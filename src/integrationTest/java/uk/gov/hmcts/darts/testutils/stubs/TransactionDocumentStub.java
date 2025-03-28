package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

@Component
@RequiredArgsConstructor
@Deprecated
public class TransactionDocumentStub {

    @Autowired
    private TranscriptionDocumentRepository transcriptionDocumentRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;

    public TranscriptionDocumentEntity createTranscriptionDocument(String fileName,
                                                                   Integer filebytes, String fileType,
                                                                   boolean hidden, TranscriptionEntity entity) {
        UserAccountEntity userAccount = userAccountRepository.findById(entity.getCreatedById()).orElseThrow();
        TranscriptionDocumentEntity documentEntity = new TranscriptionDocumentEntity();
        documentEntity.setTranscription(entity);
        documentEntity.setHidden(hidden);
        documentEntity.setFileType(fileType);
        documentEntity.setFileName(fileName);
        documentEntity.setFileSize(filebytes);
        documentEntity.setUploadedBy(userAccount);
        documentEntity.setUploadedDateTime(entity.getCreatedDateTime());
        documentEntity.setLastModifiedById(entity.getCreatedById());

        return transcriptionDocumentRepository.saveAndFlush(documentEntity);
    }
}