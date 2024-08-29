package uk.gov.hmcts.darts.common.service.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.service.CaseService;
import uk.gov.hmcts.darts.common.service.CourthouseService;
import uk.gov.hmcts.darts.common.service.CourtroomService;
import uk.gov.hmcts.darts.common.service.HearingService;
import uk.gov.hmcts.darts.common.service.JudgeService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrieveCoreObjectServiceImplTest {

    @Mock
    private HearingService hearingService;
    @Mock
    private CourthouseService courthouseService;
    @Mock
    private CourtroomService courtroomService;
    @Mock
    private CaseService caseService;
    @Mock
    private JudgeService judgeService;
    @Mock
    private AuthorisationApi authorisationApi;

    private RetrieveCoreObjectServiceImpl retrieveCoreObjectService;

    @BeforeEach
    void setUp() {
        retrieveCoreObjectService = new RetrieveCoreObjectServiceImpl(
            hearingService,
            courthouseService,
            courtroomService,
            caseService,
            judgeService,
            authorisationApi
        );
    }

    @Test
    void retrieveOrCreateHearing_shouldDelegateToHearingService() {
        String courthouseName = "Test Courthouse";
        String courtroomName = "Test Courtroom";
        String caseNumber = "Case123";
        LocalDateTime hearingDate = LocalDateTime.now();
        UserAccountEntity userAccount = new UserAccountEntity();
        HearingEntity expectedHearing = new HearingEntity();

        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);
        when(hearingService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount))
            .thenReturn(expectedHearing);

        HearingEntity result = retrieveCoreObjectService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate);

        assertEquals(expectedHearing, result);
        verify(hearingService).retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount);
    }

    @Test
    void retrieveOrCreateHearingWithMedia_shouldDelegateToHearingService() {
        String courthouseName = "Test Courthouse";
        String courtroomName = "Test Courtroom";
        String caseNumber = "Case123";
        LocalDateTime hearingDate = LocalDateTime.now();
        UserAccountEntity userAccount = new UserAccountEntity();
        MediaEntity mediaEntity = new MediaEntity();
        HearingEntity expectedHearing = new HearingEntity();

        when(hearingService.retrieveOrCreateHearingWithMedia(courthouseName, courtroomName, caseNumber, hearingDate, userAccount, mediaEntity))
            .thenReturn(expectedHearing);

        HearingEntity result = retrieveCoreObjectService.retrieveOrCreateHearingWithMedia(courthouseName, courtroomName, caseNumber, hearingDate, userAccount,
                                                                                          mediaEntity);

        assertEquals(expectedHearing, result);
        verify(hearingService).retrieveOrCreateHearingWithMedia(courthouseName, courtroomName, caseNumber, hearingDate, userAccount, mediaEntity);
    }

    @Test
    void retrieveOrCreateCourtroom_withCourthouse_shouldDelegateToCourtroomService() {
        CourthouseEntity courthouse = new CourthouseEntity();
        String courtroomName = "Test Courtroom";
        UserAccountEntity userAccount = new UserAccountEntity();
        CourtroomEntity expectedCourtroom = new CourtroomEntity();

        when(courtroomService.retrieveOrCreateCourtroom(courthouse, courtroomName, userAccount)).thenReturn(expectedCourtroom);

        CourtroomEntity result = retrieveCoreObjectService.retrieveOrCreateCourtroom(courthouse, courtroomName, userAccount);

        assertEquals(expectedCourtroom, result);
        verify(courtroomService).retrieveOrCreateCourtroom(courthouse, courtroomName, userAccount);
    }

    @Test
    void retrieveOrCreateCourtroom_withCourthouseName_shouldDelegateToCourtroomService() {
        String courthouseName = "Test Courthouse";
        String courtroomName = "Test Courtroom";
        UserAccountEntity userAccount = new UserAccountEntity();
        CourtroomEntity expectedCourtroom = new CourtroomEntity();

        when(courtroomService.retrieveOrCreateCourtroom(courthouseName, courtroomName, userAccount)).thenReturn(expectedCourtroom);

        CourtroomEntity result = retrieveCoreObjectService.retrieveOrCreateCourtroom(courthouseName, courtroomName, userAccount);

        assertEquals(expectedCourtroom, result);
        verify(courtroomService).retrieveOrCreateCourtroom(courthouseName, courtroomName, userAccount);
    }

    @Test
    void retrieveOrCreateCase_withoutUserAccount_shouldDelegateToCaseService() {
        String courthouseName = "Test Courthouse";
        String caseNumber = "Case123";
        UserAccountEntity userAccount = new UserAccountEntity();
        CourtCaseEntity expectedCase = new CourtCaseEntity();

        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);
        when(caseService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount)).thenReturn(expectedCase);

        CourtCaseEntity result = retrieveCoreObjectService.retrieveOrCreateCase(courthouseName, caseNumber);

        assertEquals(expectedCase, result);
        verify(caseService).retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
    }

    @Test
    void retrieveOrCreateCase_withUserAccount_shouldDelegateToCaseService() {
        String courthouseName = "Test Courthouse";
        String caseNumber = "Case123";
        UserAccountEntity userAccount = new UserAccountEntity();
        CourtCaseEntity expectedCase = new CourtCaseEntity();

        when(caseService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount)).thenReturn(expectedCase);

        CourtCaseEntity result = retrieveCoreObjectService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount);

        assertEquals(expectedCase, result);
        verify(caseService).retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
    }

    @Test
    void retrieveOrCreateCase_withCourthouse_shouldDelegateToCaseService() {
        CourthouseEntity courthouse = new CourthouseEntity();
        String caseNumber = "Case123";
        UserAccountEntity userAccount = new UserAccountEntity();
        CourtCaseEntity expectedCase = new CourtCaseEntity();

        when(caseService.retrieveOrCreateCase(courthouse, caseNumber, userAccount)).thenReturn(expectedCase);

        CourtCaseEntity result = retrieveCoreObjectService.retrieveOrCreateCase(courthouse, caseNumber, userAccount);

        assertEquals(expectedCase, result);
        verify(caseService).retrieveOrCreateCase(courthouse, caseNumber, userAccount);
    }

    @Test
    void retrieveCourthouse_shouldDelegateToCourthouseService() {
        String courthouseName = "Test Courthouse";
        CourthouseEntity expectedCourthouse = new CourthouseEntity();

        when(courthouseService.retrieveCourthouse(courthouseName)).thenReturn(expectedCourthouse);

        CourthouseEntity result = retrieveCoreObjectService.retrieveCourthouse(courthouseName);

        assertEquals(expectedCourthouse, result);
        verify(courthouseService).retrieveCourthouse(courthouseName);
    }

    @Test
    void retrieveOrCreateJudge_withoutUserAccount_shouldDelegateToJudgeService() {
        String judgeName = "Judge Smith";
        UserAccountEntity userAccount = new UserAccountEntity();
        JudgeEntity expectedJudge = new JudgeEntity();

        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);
        when(judgeService.retrieveOrCreateJudge(judgeName, userAccount)).thenReturn(expectedJudge);

        JudgeEntity result = retrieveCoreObjectService.retrieveOrCreateJudge(judgeName);

        assertEquals(expectedJudge, result);
        verify(judgeService).retrieveOrCreateJudge(judgeName, userAccount);
    }

    @Test
    void retrieveOrCreateJudge_withUserAccount_shouldDelegateToJudgeService() {
        String judgeName = "Judge Smith";
        UserAccountEntity userAccount = new UserAccountEntity();
        JudgeEntity expectedJudge = new JudgeEntity();

        when(judgeService.retrieveOrCreateJudge(judgeName, userAccount)).thenReturn(expectedJudge);

        JudgeEntity result = retrieveCoreObjectService.retrieveOrCreateJudge(judgeName, userAccount);

        assertEquals(expectedJudge, result);
        verify(judgeService).retrieveOrCreateJudge(judgeName, userAccount);
    }
}