package uk.gov.hmcts.darts.common.service.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.CaseCommonService;
import uk.gov.hmcts.darts.common.service.CourtroomCommonService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingServiceImplTest {

    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private CaseCommonService caseCommonService;
    @Mock
    private CourtroomCommonService courtroomCommonService;

    private HearingCommonServiceImpl hearingService;

    @BeforeEach
    void setUp() {
        hearingService = new HearingCommonServiceImpl(hearingRepository, caseCommonService, courtroomCommonService);
    }

    @Test
    void retrieveOrCreateHearingExistingHearingShouldUpdateAndReturn() {
        String courthouseName = "Test Courthouse";
        String courtroomName = "Test Courtroom";
        String caseNumber = "Case123";
        LocalDateTime hearingDate = LocalDateTime.now();
        UserAccountEntity userAccount = new UserAccountEntity();
        HearingEntity existingHearing = new HearingEntity();

        when(hearingRepository.findHearing(courthouseName, courtroomName, caseNumber, hearingDate.toLocalDate()))
            .thenReturn(Optional.of(existingHearing));
        when(hearingRepository.saveAndFlush(existingHearing)).thenReturn(existingHearing);

        HearingEntity result = hearingService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount);

        assertEquals(existingHearing, result);
        verify(hearingRepository).saveAndFlush(existingHearing);
    }

    @Test
    void retrieveOrCreateHearingNewHearingShouldCreateAndReturn() {
        String courthouseName = "Test Courthouse";
        String courtroomName = "Test Courtroom";
        String caseNumber = "Case123";
        LocalDateTime hearingDate = LocalDateTime.now();
        UserAccountEntity userAccount = new UserAccountEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        CourtroomEntity courtroom = new CourtroomEntity();
        HearingEntity newHearing = new HearingEntity();

        when(hearingRepository.findHearing(courthouseName, courtroomName, caseNumber, hearingDate.toLocalDate()))
            .thenReturn(Optional.empty());
        when(caseCommonService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount)).thenReturn(courtCase);
        when(courtroomCommonService.retrieveOrCreateCourtroom(courtCase.getCourthouse(), courtroomName, userAccount)).thenReturn(courtroom);
        when(hearingRepository.saveAndFlush(any(HearingEntity.class))).thenReturn(newHearing);

        HearingEntity result = hearingService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount);

        assertEquals(newHearing, result);
        verify(hearingRepository).saveAndFlush(any(HearingEntity.class));
    }

    @Test
    void retrieveOrCreateHearingWithMediaExistingHearingShouldUpdateAndReturn() {
        String courthouseName = "Test Courthouse";
        String courtroomName = "Test Courtroom";
        String caseNumber = "Case123";
        LocalDateTime hearingDate = LocalDateTime.now();
        UserAccountEntity userAccount = new UserAccountEntity();
        MediaEntity mediaEntity = new MediaEntity();
        HearingEntity existingHearing = new HearingEntity();

        when(hearingRepository.findHearing(courthouseName, courtroomName, caseNumber, hearingDate.toLocalDate()))
            .thenReturn(Optional.of(existingHearing));
        when(hearingRepository.saveAndFlush(existingHearing)).thenReturn(existingHearing);

        HearingEntity result = hearingService.retrieveOrCreateHearingWithMedia(courthouseName, courtroomName, caseNumber, hearingDate, userAccount,
                                                                               mediaEntity);

        assertEquals(existingHearing, result);
        verify(hearingRepository).saveAndFlush(existingHearing);
    }

    @Test
    void retrieveOrCreateHearingWithMediaNewHearingShouldCreateAndReturn() {
        String courthouseName = "Test Courthouse";
        String courtroomName = "Test Courtroom";
        String caseNumber = "Case123";
        LocalDateTime hearingDate = LocalDateTime.now();
        UserAccountEntity userAccount = new UserAccountEntity();
        MediaEntity mediaEntity = new MediaEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        CourtroomEntity courtroom = new CourtroomEntity();
        HearingEntity newHearing = new HearingEntity();

        when(hearingRepository.findHearing(courthouseName, courtroomName, caseNumber, hearingDate.toLocalDate()))
            .thenReturn(Optional.empty());
        when(caseCommonService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount)).thenReturn(courtCase);
        when(courtroomCommonService.retrieveOrCreateCourtroom(courtCase.getCourthouse(), courtroomName, userAccount)).thenReturn(courtroom);
        when(hearingRepository.saveAndFlush(any(HearingEntity.class))).thenReturn(newHearing);

        HearingEntity result = hearingService.retrieveOrCreateHearingWithMedia(courthouseName, courtroomName, caseNumber, hearingDate, userAccount,
                                                                               mediaEntity);

        assertEquals(newHearing, result);
        verify(hearingRepository).saveAndFlush(any(HearingEntity.class));
    }

    @Test
    void linkAudioToHearings_whenNullCourtCase_shouldNotLinkAndReturnFalse() {
        MediaEntity media = mock(MediaEntity.class);
        assertThat(hearingService.linkAudioToHearings(null, media))
            .isFalse();
        verifyNoInteractions(hearingRepository, media);
    }

    @Test
    void linkAudioToHearings_whenHearingNotFound_shouldNotLinkAndReturnFalse() {
        MediaEntity media = new MediaEntity();
        media.setStart(OffsetDateTime.now().minusDays(5));
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("SOME-COURTHOUSE-NAME");
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setName("SOME-ROOM-NAME");
        courtroom.setCourthouse(courthouse);
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber("SOME-COURT-CASE");
        media.setCourtroom(courtroom);

        HearingEntity hearing = new HearingEntity();
        when(hearingRepository.findHearing(any(), any(), any(), any()))
            .thenReturn(Optional.empty());

        assertThat(hearingService.linkAudioToHearings(courtCase, media)).isFalse();
        assertThat(hearing.getMediaList()).isEmpty();
        verify(hearingRepository, never()).saveAndFlush(any());
        verify(hearingRepository).findHearing(
            "SOME-COURTHOUSE-NAME",
            "SOME-ROOM-NAME",
            "SOME-COURT-CASE",
            LocalDate.now().minusDays(5)
        );
    }

    @Test
    void linkAudioToHearings_whenCourtCaseExistsAndHearingFound_shouldLinkAndReturnTrue() {
        MediaEntity media = new MediaEntity();
        media.setStart(OffsetDateTime.now().minusDays(5));
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("SOME-COURTHOUSE-NAME");
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setName("SOME-ROOM-NAME");
        courtroom.setCourthouse(courthouse);
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber("SOME-COURT-CASE");
        media.setCourtroom(courtroom);

        HearingEntity hearing = new HearingEntity();
        when(hearingRepository.findHearing(any(), any(), any(), any()))
            .thenReturn(Optional.of(hearing));

        assertThat(hearingService.linkAudioToHearings(courtCase, media)).isTrue();

        assertThat(hearing.getMediaList())
            .hasSize(1)
            .contains(media);
        assertThat(hearing.getHearingIsActual()).isTrue();

        verify(hearingRepository).saveAndFlush(hearing);
        verify(hearingRepository).findHearing(
            "SOME-COURTHOUSE-NAME",
            "SOME-ROOM-NAME",
            "SOME-COURT-CASE",
            LocalDate.now().minusDays(5)
        );
    }
}

