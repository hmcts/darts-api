package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TranscriptionEntityTest {


    @Test
    void testGetCourtCaseViaHearing() {
        var courtCase = new CourtCaseEntity();
        var hearingCourtCase = new CourtCaseEntity();
        var transcription = new TranscriptionEntity();
        transcription.setCourtCases(Set.of(courtCase));
        var hearing = new HearingEntity();
        hearing.setCourtCase(hearingCourtCase);
        transcription.setHearings(Set.of(hearing));
        assertEquals(hearingCourtCase, transcription.getCourtCase());
    }

    @Test
    void testGetCourtCaseViaHearingMultiple() {
        var courtCase = new CourtCaseEntity();
        var hearingCourtCase1 = new CourtCaseEntity();
        hearingCourtCase1.setId(1);
        hearingCourtCase1.setCreatedDateTime(OffsetDateTime.now());
        var hearingCourtCase2 = new CourtCaseEntity();
        hearingCourtCase2.setId(2);
        hearingCourtCase2.setCreatedDateTime(OffsetDateTime.now());
        var transcription = new TranscriptionEntity();
        transcription.setCourtCases(Set.of(courtCase));
        var hearing1 = new HearingEntity();
        hearing1.setId(1);
        hearing1.setCreatedDateTime(OffsetDateTime.now());
        var hearing2 = new HearingEntity();
        hearing2.setId(2);
        hearing2.setCreatedDateTime(OffsetDateTime.now());

        hearing1.setCourtCase(hearingCourtCase1);
        hearing2.setCourtCase(hearingCourtCase2);
        transcription.setHearings(Set.of(hearing1, hearing2));
        assertEquals(hearingCourtCase1, transcription.getCourtCase());
    }

    @Test
    void testGetCourtCaseDirect() {
        var courtCase = new CourtCaseEntity();
        var transcription = new TranscriptionEntity();
        transcription.setCourtCases(Set.of(courtCase));
        assertEquals(courtCase, transcription.getCourtCase());
    }

    @Test
    void testGetCourtCaseDirectMultiple() {
        var courtCase1 = new CourtCaseEntity();
        courtCase1.setId(1);
        courtCase1.setCreatedDateTime(OffsetDateTime.now());
        var courtCase2 = new CourtCaseEntity();
        courtCase2.setId(2);
        courtCase2.setCreatedDateTime(OffsetDateTime.now());
        var transcription = new TranscriptionEntity();
        transcription.setCourtCases(Set.of(courtCase1, courtCase2));
        assertEquals(courtCase1, transcription.getCourtCase());
    }

    @Test
    void testGetCourtCaseNone() {
        var transcription = new TranscriptionEntity();
        assertNull(transcription.getCourtCase());
    }

    @Test
    void testGetCourtRoomViaHearing() {
        var courtRoom = new CourtroomEntity();
        var transcription = new TranscriptionEntity();
        var hearing = new HearingEntity();
        hearing.setCourtroom(courtRoom);
        transcription.setHearings(Set.of(hearing));
        assertEquals(courtRoom, transcription.getCourtroom());
    }

    @Test
    void testGetCourtRoomViaHearingMultipleWithSetHearing() {
        final var transcription = new TranscriptionEntity();
        final var courtRoom1 = new CourtroomEntity();
        final var courtRoom2 = new CourtroomEntity();
        final var hearing1 = new HearingEntity();
        hearing1.setId(1);
        hearing1.setCreatedDateTime(OffsetDateTime.now());
        final var hearing2 = new HearingEntity();
        hearing2.setId(2);
        hearing2.setCreatedDateTime(OffsetDateTime.now());

        hearing1.setCourtroom(courtRoom1);
        hearing2.setCourtroom(courtRoom2);
        transcription.setHearings(Set.of(hearing2, hearing1));
        assertEquals(courtRoom1, transcription.getCourtroom());
    }

    @Test
    void testGetCourtRoomViaHearingMultipleWithAddHearing() {
        var transcription = new TranscriptionEntity();
        var courtRoom1 = new CourtroomEntity();
        var courtRoom2 = new CourtroomEntity();
        var hearing1 = new HearingEntity();
        var hearing2 = new HearingEntity();
        hearing1.setCourtroom(courtRoom1);
        hearing2.setCourtroom(courtRoom2);
        transcription.addHearing(hearing2);
        transcription.addHearing(hearing1);
        assertEquals(courtRoom2, transcription.getCourtroom());
    }

    @Test
    void testGetCourtRoomDirect() {
        var courtRoom = new CourtroomEntity();
        var transcription = new TranscriptionEntity();
        transcription.setCourtroom(courtRoom);
        assertEquals(courtRoom, transcription.getCourtroom());
    }

    @Test
    void testGetCourtRoomNone() {
        var transcription = new TranscriptionEntity();
        assertNull(transcription.getCourtroom());
    }

}