package uk.gov.hmcts.darts.common.service.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
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
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
        hearingService = spy(new HearingCommonServiceImpl(hearingRepository, caseCommonService, courtroomCommonService));
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
    void linkEntityToHearing_whenNullCourtCase_shouldNotLinkAndReturnFalse() {
        MediaEntity media = mock(MediaEntity.class);
        CourtroomEntity courtroom = mock(CourtroomEntity.class);
        assertThat(hearingService.linkEntityToHearing(
            "test",
            media,
            null,
            courtroom,
            LocalDate.now(),
            HearingEntity::addMedia
        )).isFalse();
        verifyNoInteractions(hearingRepository);
    }

    @Test
    void linkEntityToHearing_whenHearingNotFound_shouldNotLinkAndReturnFalse() {
        CourtroomEntity courtroom = new CourtroomEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        MediaEntity media = new MediaEntity();
        media.setStart(OffsetDateTime.now().minusDays(5));
        media.setCourtroom(courtroom);

        HearingEntity hearing = new HearingEntity();
        when(hearingRepository.findHearing(any(), any(), any()))
            .thenReturn(Optional.empty());

        assertThat(hearingService.linkEntityToHearing(
            "test",
            media,
            courtCase,
            courtroom,
            LocalDate.now().minusDays(5),
            HearingEntity::addMedia
        )).isFalse();
        assertThat(hearing.getMedias()).isEmpty();
        verify(hearingRepository, never()).saveAndFlush(any());
        verify(hearingRepository).findHearing(
            courtCase,
            courtroom,
            LocalDate.now().minusDays(5)
        );
    }

    @Test
    void linkEntityToHearing_whenCourtCaseExistsAndHearingFound_shouldLinkAndReturnTrue() {
        CourtroomEntity courtroom = new CourtroomEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        MediaEntity media = new MediaEntity();
        media.setStart(OffsetDateTime.now().minusDays(5));
        media.setCourtroom(courtroom);

        HearingEntity hearing = new HearingEntity();
        when(hearingRepository.findHearing(any(), any(), any()))
            .thenReturn(Optional.of(hearing));

        assertThat(hearingService.linkEntityToHearing(
            "test",
            media,
            courtCase,
            courtroom,
            LocalDate.now().minusDays(5),
            HearingEntity::addMedia))
            .isTrue();

        assertThat(hearing.getMedias())
            .hasSize(1)
            .contains(media);
        assertThat(hearing.getHearingIsActual()).isTrue();

        verify(hearingRepository).saveAndFlush(hearing);
        verify(hearingRepository).findHearing(
            courtCase,
            courtroom,
            LocalDate.now().minusDays(5)
        );
    }

    @Test
    void linkAudioToHearings_shouldCalllinkEntityToHearing_withCorrectValues() {
        CourtroomEntity courtroom = new CourtroomEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        MediaEntity media = new MediaEntity();
        media.setStart(OffsetDateTime.now().minusDays(5));
        media.setCourtroom(courtroom);

        doReturn(true).when(hearingService).linkEntityToHearing(
            any(), any(), any(), any(), any(), any());


        hearingService.linkAudioToHearings(courtCase, media);

        ArgumentCaptor<BiConsumer<HearingEntity, MediaEntity>> argumentCaptor = ArgumentCaptor.captor();

        verify(hearingService).linkEntityToHearing(
            eq("media"),
            eq(media),
            eq(courtCase),
            eq(courtroom),
            eq(LocalDate.now().minusDays(5)),
            argumentCaptor.capture()
        );

        BiConsumer<HearingEntity, MediaEntity> biConsumer = argumentCaptor.getValue();
        HearingEntity hearingEntity = mock(HearingEntity.class);
        biConsumer.accept(hearingEntity, media);
        verify(hearingEntity).addMedia(media);
        verifyNoMoreInteractions(hearingEntity);
    }

    @Test
    void linkEventToHearings_shouldCalllinkEntityToHearing_withCorrectValues() {
        CourtroomEntity courtroom = new CourtroomEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setTimestamp(OffsetDateTime.now().minusDays(5));
        eventEntity.setCourtroom(courtroom);

        doReturn(true).when(hearingService).linkEntityToHearing(
            any(), any(), any(), any(), any(), any());


        hearingService.linkEventToHearings(courtCase, eventEntity);

        ArgumentCaptor<BiConsumer<HearingEntity, EventEntity>> argumentCaptor = ArgumentCaptor.captor();

        verify(hearingService).linkEntityToHearing(
            eq("event"),
            eq(eventEntity),
            eq(courtCase),
            eq(courtroom),
            eq(LocalDate.now().minusDays(5)),
            argumentCaptor.capture()
        );

        BiConsumer<HearingEntity, EventEntity> biConsumer = argumentCaptor.getValue();
        HearingEntity hearingEntity = mock(HearingEntity.class);
        biConsumer.accept(hearingEntity, eventEntity);
        verify(hearingEntity).addEvent(eventEntity);
        verifyNoMoreInteractions(hearingEntity);

    }
}

