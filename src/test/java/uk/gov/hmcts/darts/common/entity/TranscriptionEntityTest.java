package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TranscriptionEntityTest {

    @Test
    void positiveAnonymize() {
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();

        TranscriptionCommentEntity transcriptionCommentEntity = mock(TranscriptionCommentEntity.class);
        TranscriptionCommentEntity transcriptionCommentEntity2 = mock(TranscriptionCommentEntity.class);

        transcriptionEntity.setTranscriptionCommentEntities(List.of(transcriptionCommentEntity, transcriptionCommentEntity2));

        UserAccountEntity userAccount = new UserAccountEntity();
        UUID uuid = UUID.randomUUID();
        transcriptionEntity.anonymize(userAccount, uuid);

        verify(transcriptionCommentEntity, times(1))
            .anonymize(userAccount, uuid);
        verify(transcriptionCommentEntity2, times(1))
            .anonymize(userAccount, uuid);
    }

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
        assertNull(transcription.getCourtCase());
    }

    @Test
    void testGetCourtRoomViaHearing() {
        var courtRoom = new CourtroomEntity();
        var transcription = new TranscriptionEntity();
        var hearing = new HearingEntity();
        hearing.setCourtroom(courtRoom);
        transcription.setHearings(List.of(hearing));
        assertEquals(courtRoom, transcription.getCourtroom());
    }

    @Test
    void testGetCourtRoomViaHearingMultipleWithSetHearing() {
        var transcription = new TranscriptionEntity();
        var courtRoom1 = new CourtroomEntity();
        var courtRoom2 = new CourtroomEntity();
        var hearing1 = new HearingEntity();
        var hearing2 = new HearingEntity();
        hearing1.setCourtroom(courtRoom1);
        hearing2.setCourtroom(courtRoom2);
        transcription.setHearings(List.of(hearing2, hearing1));
        assertEquals(courtRoom2, transcription.getCourtroom());
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