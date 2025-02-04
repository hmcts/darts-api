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
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.LocalDateTime;

public interface RetrieveCoreObjectService {

    /**
     * This method is used when events are received and processed.
     * By creating a new transaction, along with using the @Retryable annotation, we can ensure that concurrent requests to create the
     * same hearing can be processed without causing a constraint violation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class}, maxAttempts = 10)
    HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate);

    /**
     * This method is used for the following
     * - daily list processing
     * - adding audio using metadata
     * By creating a new transaction, along with using the @Retryable annotation, we can ensure that concurrent requests to create the
     * same hearing can be processed without causing a constraint violation.
     * Concurrency isn't a concern for daily list processing.
     */
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class}, maxAttempts = 10)
    HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                          UserAccountEntity userAccount);

    /**
     * Retrieve or create a case and link to media.
     * @deprecated This method is only used by tests. Tests should be refactored and this method should be removed.
     */
    @Deprecated(since = "04/02/2025")
    HearingEntity retrieveOrCreateHearingWithMedia(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                   UserAccountEntity userAccount, MediaEntity mediaEntity);

    /**
     * Retrieve or create a courtroom.
     * @deprecated This method is only used by tests.
     *     Tests should be refactored to use the other `retrieveOrCreateCourtroom` method, and this method should be removed.
     */
    @Deprecated(since = "04/02/2025")
    CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount);

    /**
     * This method is used for the following
     * - node register
     * - events
     * - get cases
     * - adding audio
     * - audio requests
     * By creating a new transaction, along with using the @Retryable annotation, we can ensure that concurrent requests that attempt to create the
     * same courtroom can be processed without causing a constraint violation.
     * Concurrency isn't a concern for adding audio or audio requests. Due to the nature of these calls, it is extremely unlikely that the courtroom
     * will not exist.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class}, maxAttempts = 10)
    CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName, UserAccountEntity userAccount);

    /**
     * This method is used for the following
     * - add case
     * - events
     * By creating a new transaction, along with using the @Retryable annotation, we can ensure that concurrent requests that attempt to create the
     * same case can be processed without causing a constraint violation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class}, maxAttempts = 10)
    CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber);

    /**
     * Retrieve or create a case.
     * @deprecated This method is only used by tests.
     *     Tests should be refactored to use the other `retrieveOrCreateCase` method, and this method should be removed.
     */
    @Deprecated(since = "04/02/2025")
    CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber, UserAccountEntity userAccount);

    /**
     * This method is used during add audio when retrieving/creating cases used in the metadata before adding to the media_linked_case table.
     * It's possible that the same case could be sent in concurrent add audio requests.
     * By creating a new transaction, along with using the @Retryable annotation, we can ensure that concurrent requests that attempt to create the
     * same case can be processed without causing a constraint violation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class}, maxAttempts = 10)
    CourtCaseEntity retrieveOrCreateCase(CourthouseEntity courthouse, String caseNumber, UserAccountEntity userAccount);

    /**
     * Used to retrieve the courthouse during add audio validation.
     */
    CourthouseEntity retrieveCourthouse(String courthouseName);

    /**
     * This method is used for add case, it's possible that the same judge could be sent in concurrent add case requests.
     * By creating a new transaction, along with using the @Retryable annotation, we can ensure that concurrent requests that attempt to create the
     * same judge can be processed without causing a constraint violation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class}, maxAttempts = 10)
    JudgeEntity retrieveOrCreateJudge(String judgeName);

    /**
     * This method is only used to add judges during daily list processing. This task processed data in serial and therefore
     * there are no concurrent write concerns.
     */
    JudgeEntity retrieveOrCreateJudge(String judgeName, UserAccountEntity userAccount);

}