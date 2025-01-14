package uk.gov.hmcts.darts.cases.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class CaseServiceGetTranscriptsTest extends IntegrationBase {

    @Autowired
    private CaseService service;
    private int caseId;

    private static final String CASE_NUMBER = "CASE1";
    private static final String COURTHOUSE = "SWANSEA";
    private static final String COURTROOM = "1";
    private static final OffsetDateTime DATE_TIME = OffsetDateTime.of(2023, 6, 20, 10, 1, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setupData() {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            CASE_NUMBER,
            COURTHOUSE,
            COURTROOM,
            DateConverterUtil.toLocalDateTime(OffsetDateTime.parse("2023-01-01T12:00Z"))
        );
        CourtCaseEntity courtCaseEntity = hearingEntity.getCourtCase();
        caseId = courtCaseEntity.getId();

        // transcription with 0 docs - should be visible
        // linked to case
        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createTranscription(courtCaseEntity);
        transcription.setId(1);
        transcription.setCreatedDateTime(DATE_TIME);
        transcription.setIsManualTranscription(true);
        dartsDatabase.save(transcription);
        createTranscriptionDocs(transcription, List.of());

        // transcription with 3 docs, none hidden - should be visible
        // linked to case
        TranscriptionEntity transcription2 = dartsDatabase.getTranscriptionStub().createTranscription(courtCaseEntity);
        transcription2.setId(1);
        transcription2.setCreatedDateTime(DATE_TIME);
        transcription2.setIsManualTranscription(true);
        dartsDatabase.save(transcription2);
        createTranscriptionDocs(transcription2, List.of(false, false, false));

        // transcription with 3 docs, 1 hidden - should be visible
        // linked to hearing
        TranscriptionEntity transcription3 = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        transcription3.setId(3);
        transcription3.setCreatedDateTime(DATE_TIME);
        transcription3.setIsManualTranscription(true);
        dartsDatabase.save(transcription3);
        createTranscriptionDocs(transcription3, List.of(false, true, false));

        // transcription with 3 docs, all hidden - should be hidden
        // linked to hearing
        TranscriptionEntity transcription4 = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        transcription3.setId(4);
        transcription4.setCreatedDateTime(DATE_TIME);
        transcription4.setIsManualTranscription(true);
        dartsDatabase.save(transcription4);
        createTranscriptionDocs(transcription4, List.of(true, true, true));
    }

    @Test
    void testGetTranscriptsByCaseId() {
        var caseTranscripts = service.getTranscriptsByCaseId(caseId);
        assertEquals(3, caseTranscripts.size());
        assertThat(caseTranscripts.get(0).getRequestedOn()).isAfterOrEqualTo(caseTranscripts.get(1).getRequestedOn());
        assertThat(caseTranscripts.get(1).getRequestedOn()).isAfterOrEqualTo(caseTranscripts.get(2).getRequestedOn());

        assertEquals(2, caseTranscripts.get(0).getTranscriptionId());
        assertEquals(1, caseTranscripts.get(1).getTranscriptionId());
        assertEquals(3, caseTranscripts.get(2).getTranscriptionId());
    }

    private void createTranscriptionDocs(TranscriptionEntity transcriptionEntity, List<Boolean> hiddenList) {
        for (Boolean shouldBeHidden : hiddenList) {
            TranscriptionDocumentEntity transDoc = dartsDatabase.getTranscriptionDocumentStub().createTranscriptionDocumentForTranscription(
                transcriptionEntity);
            transDoc.setHidden(shouldBeHidden);
            dartsDatabase.getTranscriptionDocumentRepository().save(transDoc);
        }
    }
}
