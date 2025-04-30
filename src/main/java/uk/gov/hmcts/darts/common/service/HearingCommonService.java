package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.LocalDateTime;

public interface HearingCommonService {

    HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                          UserAccountEntity userAccount);

    HearingEntity retrieveOrCreateHearingWithMedia(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                   UserAccountEntity userAccount, MediaEntity mediaEntity);

    boolean linkAudioToHearings(CourtCaseEntity courtCaseEntity, MediaEntity mediaEntity);

    boolean linkEventToHearings(CourtCaseEntity courtCaseEntity, EventEntity eventEntity);
}
