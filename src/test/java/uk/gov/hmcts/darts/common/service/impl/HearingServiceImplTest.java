package uk.gov.hmcts.darts.common.service.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.CaseCommonService;
import uk.gov.hmcts.darts.common.service.CourtroomCommonService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void retrieveOrCreateHearingExistingHearing() {
        String courthouseName = "Test Courthouse";
        String courtroomName = "Test Courtroom";
        String caseNumber = "Case123";
        LocalDateTime hearingDate = LocalDateTime.now();
        UserAccountEntity userAccount = new UserAccountEntity();
        HearingEntity existingHearing = new HearingEntity();

        when(hearingRepository.findHearing(courthouseName, courtroomName, caseNumber, hearingDate.toLocalDate()))
            .thenReturn(Optional.of(existingHearing));

        HearingEntity result = hearingService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount);

        assertEquals(existingHearing, result);
        verify(hearingRepository, never()).saveAndFlush(any());
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
    void retrieveOrCreateHearingWithMediaExistingHearingShouldReturn() {
        String courthouseName = "Test Courthouse";
        String courtroomName = "Test Courtroom";
        String caseNumber = "Case123";
        LocalDateTime hearingDate = LocalDateTime.now();
        UserAccountEntity userAccount = new UserAccountEntity();
        MediaEntity mediaEntity = new MediaEntity();
        HearingEntity existingHearing = new HearingEntity();

        when(hearingRepository.findHearing(courthouseName, courtroomName, caseNumber, hearingDate.toLocalDate()))
            .thenReturn(Optional.of(existingHearing));

        HearingEntity result = hearingService.retrieveOrCreateHearingWithMedia(courthouseName, courtroomName, caseNumber, hearingDate, userAccount,
                                                                               mediaEntity);

        assertEquals(existingHearing, result);
        verify(hearingRepository, never()).saveAndFlush(any());
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
}

