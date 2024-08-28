package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class RetrieveCoreObjectServiceImpl implements RetrieveCoreObjectService {

    private final HearingService hearingService;
    private final CaseService caseService;
    private final CourtroomService courtroomService;
    private final CourthouseService courthouseService;
    private final JudgeService judgeService;


    @Override
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate) {
        return hearingService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate);
    }

    @Override
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                 UserAccountEntity userAccount) {
        return hearingService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount);
    }

    @Override
    public HearingEntity retrieveOrCreateHearingWithMedia(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                          UserAccountEntity userAccount, MediaEntity mediaEntity) {
        return hearingService.retrieveOrCreateHearingWithMedia(courthouseName, courtroomName, caseNumber, hearingDate, userAccount, mediaEntity);
    }

    @Override
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber, UserAccountEntity userAccount) {
        return caseService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
    }

    @Override
    public CourtCaseEntity retrieveOrCreateCase(CourthouseEntity courthouse, String caseNumber, UserAccountEntity userAccount) {
        return caseService.retrieveOrCreateCase(courthouse, caseNumber, userAccount);
    }

    @Override
    public CourthouseEntity retrieveCourthouse(String courthouseName) {
        return courthouseService.retrieveCourthouse(courthouseName);
    }

    @Override
    public JudgeEntity retrieveOrCreateJudge(String judgeName) {
        return null;
    }

    @Override
    public JudgeEntity retrieveOrCreateJudge(String judgeName, UserAccountEntity userAccount) {
        return judgeService.retrieveOrCreateJudge(judgeName, userAccount);
    }

    @Override
    public CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount) {
        return courtroomService.retrieveOrCreateCourtroom(courthouse, courtroomName, userAccount);
    }

    @Override
    public CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName, UserAccountEntity userAccount) {
        return courtroomService.retrieveOrCreateCourtroom(courthouseName, courtroomName, userAccount);
    }

    @Override
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber) {
        return null;
    }
}