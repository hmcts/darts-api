package uk.gov.hmcts.darts.common.service;

import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.LocalDateTime;

public interface RetrieveCoreObjectService {

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                          UserAccountEntity userAccount);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    HearingEntity retrieveOrCreateHearingWithMedia(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                   UserAccountEntity userAccount, MediaEntity mediaEntity);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName, UserAccountEntity userAccount);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber, UserAccountEntity userAccount);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    CourtCaseEntity retrieveOrCreateCase(CourthouseEntity courthouse, String caseNumber, UserAccountEntity userAccount);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    CourthouseEntity retrieveCourthouse(String courthouseName);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    JudgeEntity retrieveOrCreateJudge(String judgeName);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    JudgeEntity retrieveOrCreateJudge(String judgeName, UserAccountEntity userAccount);

}