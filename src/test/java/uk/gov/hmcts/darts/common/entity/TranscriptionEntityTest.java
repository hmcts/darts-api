package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranscriptionEntityTest {

    @Test
    void testGetCourtCaseViaHearing() {
        var courtCase = new CourtCaseEntity();
        var hearingCourtCase = new CourtCaseEntity();
        var transcription = new TranscriptionEntity();
        transcription.setCourtCases(List.of(courtCase));
        var hearing = new HearingEntity();
        hearing.setCourtCase(hearingCourtCase);
        transcription.setHearings(List.of(hearing));
        assertEquals(hearingCourtCase, transcription.getCourtCase());
    }

    @Test
    void testGetCourtCaseViaHearingMultiple() {
        var courtCase = new CourtCaseEntity();
        var hearingCourtCase1 = new CourtCaseEntity();
        var hearingCourtCase2 = new CourtCaseEntity();
        var transcription = new TranscriptionEntity();
        transcription.setCourtCases(List.of(courtCase));
        var hearing1 = new HearingEntity();
        var hearing2 = new HearingEntity();
        hearing1.setCourtCase(hearingCourtCase1);
        hearing2.setCourtCase(hearingCourtCase2);
        transcription.setHearings(List.of(hearing1, hearing2));
        assertEquals(hearingCourtCase1, transcription.getCourtCase());
    }

    @Test
    void testGetCourtCaseDirect() {
        var courtCase = new CourtCaseEntity();
        var transcription = new TranscriptionEntity();
        transcription.setCourtCases(List.of(courtCase));
        assertEquals(courtCase, transcription.getCourtCase());
    }

    @Test
    void testGetCourtCaseDirectMultiple() {
        var courtCase1 = new CourtCaseEntity();
        var courtCase2 = new CourtCaseEntity();
        var transcription = new TranscriptionEntity();
        transcription.setCourtCases(List.of(courtCase1, courtCase2));
        assertEquals(courtCase1, transcription.getCourtCase());
    }

    @Test
    void testGetCourtCaseNone() {
        var transcription = new TranscriptionEntity();
        assertEquals(null, transcription.getCourtCase());
    }

}