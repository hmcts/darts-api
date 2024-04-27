package uk.gov.hmcts.darts.common.service;

import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.LocalDateTime;

public interface RetrieveCoreObjectService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                          UserAccountEntity userAccount);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber, UserAccountEntity userAccount);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    CourthouseEntity retrieveCourthouse(String courthouseName);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    JudgeEntity retrieveOrCreateJudge(String judgeName);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class})
    JudgeEntity retrieveOrCreateJudge(String judgeName, UserAccountEntity userAccount);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    DefenceEntity createDefence(String defence, CourtCaseEntity courtCase, UserAccountEntity userAccount);

    DefendantEntity createDefendant(String defendant, CourtCaseEntity courtCase, UserAccountEntity userAccount);

    ProsecutorEntity createProsecutor(String prosecution, CourtCaseEntity courtCase, UserAccountEntity userAccount);
}
