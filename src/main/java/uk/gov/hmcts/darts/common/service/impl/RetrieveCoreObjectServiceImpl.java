package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")//TODO - refactor to reduce methods when this class is next edited
public class RetrieveCoreObjectServiceImpl implements RetrieveCoreObjectService {

    private final HearingCommonService hearingCommonService;
    private final CourthouseCommonService courthouseCommonService;
    private final CourtroomCommonService courtroomCommonService;
    private final CaseCommonService caseCommonService;
    private final JudgeCommonService judgeCommonService;
    private final AuthorisationApi authorisationApi;

    @Override
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate) {
        UserAccountEntity userAccount = authorisationApi.getCurrentUser();
        return hearingCommonService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount);
    }

    @Override
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                 UserAccountEntity userAccount) {
        return hearingCommonService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount);
    }

    @Override
    public HearingEntity retrieveOrCreateHearingWithMedia(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                          UserAccountEntity userAccount, MediaEntity mediaEntity) {
        return hearingCommonService.retrieveOrCreateHearingWithMedia(courthouseName, courtroomName, caseNumber, hearingDate, userAccount, mediaEntity);
    }

    @Override
    public CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount) {
        return courtroomCommonService.retrieveOrCreateCourtroom(courthouse, courtroomName, userAccount);
    }

    @Override
    public CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName, UserAccountEntity userAccount) {
        return courtroomCommonService.retrieveOrCreateCourtroom(courthouseName, courtroomName, userAccount);
    }

    @Override
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber) {
        UserAccountEntity userAccount = authorisationApi.getCurrentUser();
        return caseCommonService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
    }

    @Override
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber, UserAccountEntity userAccount) {
        return caseCommonService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
    }

    @Override
    public CourtCaseEntity retrieveOrCreateCase(CourthouseEntity courthouse, String caseNumber, UserAccountEntity userAccount) {
        return caseCommonService.retrieveOrCreateCase(courthouse, caseNumber, userAccount);
    }

    @Override
    public CourthouseEntity retrieveCourthouse(String courthouseName) {
        return courthouseCommonService.retrieveCourthouse(courthouseName);
    }

    @Override
    public JudgeEntity retrieveOrCreateJudge(String judgeName) {
        UserAccountEntity userAccount = authorisationApi.getCurrentUser();
        return judgeCommonService.retrieveOrCreateJudge(judgeName, userAccount);
    }

    @Override
    public JudgeEntity retrieveOrCreateJudge(String judgeName, UserAccountEntity userAccount) {
        return judgeCommonService.retrieveOrCreateJudge(judgeName, userAccount);
    }
}