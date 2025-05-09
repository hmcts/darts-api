package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.LocalDateTime;

@SuppressWarnings("PMD.TooManyMethods")//TODO - refactor to reduce methods when this class is next edited
public interface RetrieveCoreObjectService {

    HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate);

    HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                          UserAccountEntity userAccount);

    HearingEntity retrieveOrCreateHearingWithMedia(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                   UserAccountEntity userAccount, MediaEntity mediaEntity);

    CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount);

    CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName, UserAccountEntity userAccount);

    CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber);

    CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber, UserAccountEntity userAccount);

    CourtCaseEntity retrieveOrCreateCase(CourthouseEntity courthouse, String caseNumber, UserAccountEntity userAccount);

    CourthouseEntity retrieveCourthouse(String courthouseName);

    JudgeEntity retrieveOrCreateJudge(String judgeName);

    JudgeEntity retrieveOrCreateJudge(String judgeName, UserAccountEntity userAccount);

}