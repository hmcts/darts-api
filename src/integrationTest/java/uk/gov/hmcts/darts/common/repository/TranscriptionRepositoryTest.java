package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionDocumentStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.time.OffsetDateTime;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.test.common.data.TranscriptionDocumentTestData.minimalTranscriptionDocument;

class TranscriptionRepositoryTest extends IntegrationBase {

    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";
    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");

    @Autowired
    private TranscriptionRepository transcriptionRepository;

    @Autowired
    private TranscriptionStub transcriptionStub;

    @Autowired
    private TranscriptionDocumentStub transcriptionDocumentStub;

    private CourtCaseEntity courtCaseEntity;

    private int caseId;

    @BeforeEach
    public void setupData() {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );
        courtCaseEntity = hearingEntity.getCourtCase();
        caseId = courtCaseEntity.getId();

        createTranscriptionWithDocument(hearingEntity, false);
        createTranscriptionWithDocument(hearingEntity, true);
        createTranscriptionWithDocument(courtCaseEntity, false);
        createTranscriptionWithDocument(courtCaseEntity, true);
    }

    @Test
    void doesNotShowAutomated() {
        TranscriptionEntity legacyTranscription = createTranscriptionWithDocument(courtCaseEntity, false);
        legacyTranscription.setIsManualTranscription(false);
        dartsDatabase.save(legacyTranscription);

        List<TranscriptionEntity> transcriptionEntities = transcriptionRepository.findByCaseIdManualOrLegacy(caseId, true);
        assertEquals(4, transcriptionEntities.size());
    }

    @Test
    void showsLegacyAutomated() {
        TranscriptionEntity legacyTranscription = createTranscriptionWithDocument(courtCaseEntity, false);
        legacyTranscription.setLegacyObjectId("legacy");
        legacyTranscription.setIsManualTranscription(false);
        List<TranscriptionEntity> transcriptionEntities = transcriptionRepository.findByCaseIdManualOrLegacy(caseId, true);
        assertEquals(5, transcriptionEntities.size());
    }

    @Test
    void includesHidden() {
        List<TranscriptionEntity> transcriptionEntities = transcriptionRepository.findByCaseIdManualOrLegacy(caseId, true);
        assertEquals(4, transcriptionEntities.size());
    }

    @Test
    void excludesHidden() {
        var courtCase = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();
        persistTwoHiddenTwoNotHiddenTranscriptionsFor(courtCase);

        var transcriptionEntities = transcriptionRepository.findByCaseIdManualOrLegacy(courtCase.getId(), false);

        assertEquals(2, transcriptionEntities.size());
    }

    private void persistTwoHiddenTwoNotHiddenTranscriptionsFor(CourtCaseEntity courtCaseEntity) {
        range(0, 4)
            .forEach(i -> {
                var transcriptionDocument = minimalTranscriptionDocument();
                var transcription = transcriptionDocument.getTranscription();
                transcription.setIsManualTranscription(true);
                transcription.setCourtCases(asList(courtCaseEntity));
                transcriptionDocument.setHidden(i % 2 == 0);
                dartsDatabase.save(transcription);
            });
    }

    private void createTranscriptionWithDocument(HearingEntity hearingEntity, Boolean hiddenDoc) {
        TranscriptionEntity transcription = transcriptionStub.createTranscription(hearingEntity);
        createTranscriptionDocument(transcription, hiddenDoc);
    }

    private TranscriptionEntity createTranscriptionWithDocument(CourtCaseEntity courtCaseEntity, Boolean hiddenDoc) {
        TranscriptionEntity transcription = transcriptionStub.createTranscription(courtCaseEntity);
        createTranscriptionDocument(transcription, hiddenDoc);
        return transcription;
    }

    private void createTranscriptionDocument(TranscriptionEntity transcription, Boolean hiddenDoc) {
        TranscriptionDocumentEntity transcriptionDocument = transcriptionDocumentStub.createTranscriptionDocumentForTranscription(transcription);
        transcriptionDocument.setHidden(hiddenDoc);
    }
}