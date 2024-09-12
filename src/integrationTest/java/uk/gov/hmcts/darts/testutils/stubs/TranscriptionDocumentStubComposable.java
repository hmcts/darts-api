package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Deprecated
public class TranscriptionDocumentStubComposable {

    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final UserAccountRepository userAccountRepository;

    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");

    public TranscriptionDocumentEntity createTranscriptionDocumentForTranscription(UserAccountStubComposable userAccountStubComposable,
                                                                                   DartsDatabaseComposable dartsDatabaseComposable,
                                                                                   TranscriptionStubComposable transcriptionStubComposable,
                                                                                   CourthouseStubComposable courthouseStubComposable,
                                                                                   UserAccountEntity userAccountEntity
    ) {
        HearingEntity hearingEntity = dartsDatabaseComposable.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            courthouseStubComposable,
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );

        TranscriptionEntity transcriptionEntity = transcriptionStubComposable.createTranscription(userAccountStubComposable, hearingEntity, userAccountEntity);
        TranscriptionDocumentEntity transcriptionDocument = createTranscriptionDocumentForTranscription(transcriptionEntity);

        UserAccountEntity userAccount = userAccountRepository.getReferenceById(0);
        transcriptionDocument.setUploadedBy(userAccount);
        transcriptionDocument.setLastModifiedBy(userAccount);

        transcriptionDocumentRepository.saveAndFlush(transcriptionDocument);
        return transcriptionDocument;
    }

    public TranscriptionDocumentEntity createTranscriptionDocumentForTranscription(TranscriptionEntity transcriptionEntity) {
        TranscriptionDocumentEntity transcriptionDocument = new TranscriptionDocumentEntity();
        transcriptionDocument.setTranscription(transcriptionEntity);
        transcriptionDocument.setFileName("aFilename");
        transcriptionDocument.setFileType("aFileType");
        transcriptionDocument.setFileSize(100);
        transcriptionDocument.setChecksum("");

        UserAccountEntity userAccount = userAccountRepository.getReferenceById(0);
        transcriptionDocument.setUploadedBy(userAccount);
        transcriptionDocument.setLastModifiedBy(userAccount);

        transcriptionDocumentRepository.saveAndFlush(transcriptionDocument);
        return transcriptionDocument;
    }
}