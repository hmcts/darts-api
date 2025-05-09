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
import uk.gov.hmcts.darts.common.service.CaseCommonService;
import uk.gov.hmcts.darts.common.service.CourthouseCommonService;
import uk.gov.hmcts.darts.common.service.CourtroomCommonService;
import uk.gov.hmcts.darts.common.service.HearingCommonService;
import uk.gov.hmcts.darts.common.service.JudgeCommonService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrieveCoreObjectServiceImplTest {

    @Mock
    private HearingCommonService hearingCommonService;
    @Mock
    private CourthouseCommonService courthouseCommonService;
    @Mock
    private CourtroomCommonService courtroomCommonService;
    @Mock
    private CaseCommonService caseCommonService;
    @Mock
    private JudgeCommonService judgeCommonService;
    @Mock
    private AuthorisationApi authorisationApi;

    private RetrieveCoreObjectServiceSupport retrieveCoreObjectService;

    @BeforeEach
    void setUp() {
        retrieveCoreObjectService = new RetrieveCoreObjectServiceSupport(
            hearingCommonService,
            courthouseCommonService,
            courtroomCommonService,
            caseCommonService,
            judgeCommonService,
            authorisationApi
        );
    }

    @Test
    void retrieveOrCreateHearingShouldDelegateToHearingService() {
        String courthouseName = "Test Courthouse";
        String courtroomName = "Test Courtroom";
        String caseNumber = "Case123";
        LocalDateTime hearingDate = LocalDateTime.now();
        UserAccountEntity userAccount = new UserAccountEntity();
        HearingEntity expectedHearing = new HearingEntity();

        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);
        when(hearingCommonService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount))
            .thenReturn(expectedHearing);

        HearingEntity result = retrieveCoreObjectService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate);

        assertEquals(expectedHearing, result);
        verify(hearingCommonService).retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount);
    }

    @Test
    void retrieveOrCreateHearingWithMediaShouldDelegateToHearingService() {
        String courthouseName = "Test Courthouse";
        String courtroomName = "Test Courtroom";
        String caseNumber = "Case123";
        LocalDateTime hearingDate = LocalDateTime.now();
        UserAccountEntity userAccount = new UserAccountEntity();
        MediaEntity mediaEntity = new MediaEntity();
        HearingEntity expectedHearing = new HearingEntity();

        when(hearingCommonService.retrieveOrCreateHearingWithMedia(courthouseName, courtroomName, caseNumber, hearingDate, userAccount, mediaEntity))
            .thenReturn(expectedHearing);

        HearingEntity result = retrieveCoreObjectService.retrieveOrCreateHearingWithMedia(courthouseName, courtroomName, caseNumber, hearingDate, userAccount,
                                                                                          mediaEntity);

        assertEquals(expectedHearing, result);
        verify(hearingCommonService).retrieveOrCreateHearingWithMedia(courthouseName, courtroomName, caseNumber, hearingDate, userAccount, mediaEntity);
    }

    @Test
    void retrieveOrCreateCourtroomWithCourthouseShouldDelegateToCourtroomService() {
        CourthouseEntity courthouse = new CourthouseEntity();
        String courtroomName = "Test Courtroom";
        UserAccountEntity userAccount = new UserAccountEntity();
        CourtroomEntity expectedCourtroom = new CourtroomEntity();

        when(courtroomCommonService.retrieveOrCreateCourtroom(courthouse, courtroomName, userAccount)).thenReturn(expectedCourtroom);

        CourtroomEntity result = retrieveCoreObjectService.retrieveOrCreateCourtroom(courthouse, courtroomName, userAccount);

        assertEquals(expectedCourtroom, result);
        verify(courtroomCommonService).retrieveOrCreateCourtroom(courthouse, courtroomName, userAccount);
    }

    @Test
    void retrieveOrCreateCourtroomWithCourthouseNameShouldDelegateToCourtroomService() {
        String courthouseName = "Test Courthouse";
        String courtroomName = "Test Courtroom";
        UserAccountEntity userAccount = new UserAccountEntity();
        CourtroomEntity expectedCourtroom = new CourtroomEntity();

        when(courtroomCommonService.retrieveOrCreateCourtroom(courthouseName, courtroomName, userAccount)).thenReturn(expectedCourtroom);

        CourtroomEntity result = retrieveCoreObjectService.retrieveOrCreateCourtroom(courthouseName, courtroomName, userAccount);

        assertEquals(expectedCourtroom, result);
        verify(courtroomCommonService).retrieveOrCreateCourtroom(courthouseName, courtroomName, userAccount);
    }

    @Test
    void retrieveOrCreateCaseWithoutUserAccountShouldDelegateToCaseService() {
        String courthouseName = "Test Courthouse";
        String caseNumber = "Case123";
        UserAccountEntity userAccount = new UserAccountEntity();
        CourtCaseEntity expectedCase = new CourtCaseEntity();

        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);
        when(caseCommonService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount)).thenReturn(expectedCase);

        CourtCaseEntity result = retrieveCoreObjectService.retrieveOrCreateCase(courthouseName, caseNumber);

        assertEquals(expectedCase, result);
        verify(caseCommonService).retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
    }

    @Test
    void retrieveOrCreateCaseWithUserAccountShouldDelegateToCaseService() {
        String courthouseName = "Test Courthouse";
        String caseNumber = "Case123";
        UserAccountEntity userAccount = new UserAccountEntity();
        CourtCaseEntity expectedCase = new CourtCaseEntity();

        when(caseCommonService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount)).thenReturn(expectedCase);

        CourtCaseEntity result = retrieveCoreObjectService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount);

        assertEquals(expectedCase, result);
        verify(caseCommonService).retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
    }

    @Test
    void retrieveOrCreateCaseWithCourthouseShouldDelegateToCaseService() {
        CourthouseEntity courthouse = new CourthouseEntity();
        String caseNumber = "Case123";
        UserAccountEntity userAccount = new UserAccountEntity();
        CourtCaseEntity expectedCase = new CourtCaseEntity();

        when(caseCommonService.retrieveOrCreateCase(courthouse, caseNumber, userAccount)).thenReturn(expectedCase);

        CourtCaseEntity result = retrieveCoreObjectService.retrieveOrCreateCase(courthouse, caseNumber, userAccount);

        assertEquals(expectedCase, result);
        verify(caseCommonService).retrieveOrCreateCase(courthouse, caseNumber, userAccount);
    }

    @Test
    void retrieveCourthouseShouldDelegateToCourthouseService() {
        String courthouseName = "Test Courthouse";
        CourthouseEntity expectedCourthouse = new CourthouseEntity();

        when(courthouseCommonService.retrieveCourthouse(courthouseName)).thenReturn(expectedCourthouse);

        CourthouseEntity result = retrieveCoreObjectService.retrieveCourthouse(courthouseName);

        assertEquals(expectedCourthouse, result);
        verify(courthouseCommonService).retrieveCourthouse(courthouseName);
    }

    @Test
    void retrieveOrCreateJudgeWithoutUserAccountShouldDelegateToJudgeService() {
        String judgeName = "Judge Smith";
        UserAccountEntity userAccount = new UserAccountEntity();
        JudgeEntity expectedJudge = new JudgeEntity();

        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);
        when(judgeCommonService.retrieveOrCreateJudge(judgeName, userAccount)).thenReturn(expectedJudge);

        JudgeEntity result = retrieveCoreObjectService.retrieveOrCreateJudge(judgeName);

        assertEquals(expectedJudge, result);
        verify(judgeCommonService).retrieveOrCreateJudge(judgeName, userAccount);
    }

    @Test
    void retrieveOrCreateJudgeWithUserAccountShouldDelegateToJudgeService() {
        String judgeName = "Judge Smith";
        UserAccountEntity userAccount = new UserAccountEntity();
        JudgeEntity expectedJudge = new JudgeEntity();

        when(judgeCommonService.retrieveOrCreateJudge(judgeName, userAccount)).thenReturn(expectedJudge);

        JudgeEntity result = retrieveCoreObjectService.retrieveOrCreateJudge(judgeName, userAccount);

        assertEquals(expectedJudge, result);
        verify(judgeCommonService).retrieveOrCreateJudge(judgeName, userAccount);
    }
}