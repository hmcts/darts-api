package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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

@RequiredArgsConstructor
@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")//TODO - refactor to reduce methods when this class is next edited
/**
 * Please use RetrieveCoreObjectServiceTransactionalSupport instead of this class, as this beter supports transaction state.
 */
public class RetrieveCoreObjectServiceSupport {

    private final HearingCommonService hearingCommonService;
    private final CourthouseCommonService courthouseCommonService;
    private final CourtroomCommonService courtroomCommonService;
    private final CaseCommonService caseCommonService;
    private final JudgeCommonService judgeCommonService;
    private final AuthorisationApi authorisationApi;

    /**
     * This method is used when events are received and processed.
     * By creating a new transaction, along with using the @Retryable annotation, we can ensure that concurrent requests to create the
     * same hearing can be processed without causing a constraint violation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class}, maxAttempts = 10)
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate) {
        UserAccountEntity userAccount = authorisationApi.getCurrentUser();
        return hearingCommonService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount);
    }

    /**
     * This method is used for the following
     * - daily list processing
     * - adding audio using metadata
     * By creating a new transaction, along with using the @Retryable annotation, we can ensure that concurrent requests to create the
     * same hearing can be processed without causing a constraint violation.
     * Concurrency isn't a concern for daily list processing.
     */
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class}, maxAttempts = 10)
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                 UserAccountEntity userAccount) {
        return hearingCommonService.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount);
    }

    /**
     * Retrieve or create a case and link to media.
     *
     * @deprecated This method is only used by tests. Tests should be refactored and this method should be removed.
     */
    @SuppressWarnings("java:S1133") // suppress sonar warning about deprecated methods
    @Deprecated(since = "04/02/2025")
    public HearingEntity retrieveOrCreateHearingWithMedia(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                          UserAccountEntity userAccount, MediaEntity mediaEntity) {
        return hearingCommonService.retrieveOrCreateHearingWithMedia(courthouseName, courtroomName, caseNumber, hearingDate, userAccount, mediaEntity);
    }

    /**
     * Retrieve or create a courtroom.
     *
     * @deprecated This method is only used by tests.
     *     Tests should be refactored to use the other `retrieveOrCreateCourtroom` method, and this method should be removed.
     */
    @SuppressWarnings("java:S1133") // suppress sonar warning about deprecated methods
    @Deprecated(since = "04/02/2025")
    public CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount) {
        return courtroomCommonService.retrieveOrCreateCourtroom(courthouse, courtroomName, userAccount);
    }

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
    public CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName, UserAccountEntity userAccount) {
        return courtroomCommonService.retrieveOrCreateCourtroom(courthouseName, courtroomName, userAccount);
    }

    /**
     * This method is used for the following
     * - add case
     * - events
     * By creating a new transaction, along with using the @Retryable annotation, we can ensure that concurrent requests that attempt to create the
     * same case can be processed without causing a constraint violation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class}, maxAttempts = 10)
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber) {
        UserAccountEntity userAccount = authorisationApi.getCurrentUser();
        return caseCommonService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
    }

    /**
     * Retrieve or create a case.
     *
     * @deprecated This method is only used by tests.
     *     Tests should be refactored to use the other `retrieveOrCreateCase` method, and this method should be removed.
     */
    @SuppressWarnings("java:S1133") // suppress sonar warning about deprecated methods
    @Deprecated(since = "04/02/2025")
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber, UserAccountEntity userAccount) {
        return caseCommonService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
    }

    /**
     * This method is used during add audio when retrieving/creating cases used in the metadata before adding to the media_linked_case table.
     * It's possible that the same case could be sent in concurrent add audio requests.
     * By creating a new transaction, along with using the @Retryable annotation, we can ensure that concurrent requests that attempt to create the
     * same case can be processed without causing a constraint violation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class}, maxAttempts = 10)
    public CourtCaseEntity retrieveOrCreateCase(CourthouseEntity courthouse, String caseNumber, UserAccountEntity userAccount) {
        return caseCommonService.retrieveOrCreateCase(courthouse, caseNumber, userAccount);
    }

    /**
     * Used to retrieve the courthouse during add audio validation.
     */
    public CourthouseEntity retrieveCourthouse(String courthouseName) {
        return courthouseCommonService.retrieveCourthouse(courthouseName);
    }

    /**
     * This method is used for add case, it's possible that the same judge could be sent in concurrent add case requests.
     * By creating a new transaction, along with using the @Retryable annotation, we can ensure that concurrent requests that attempt to create the
     * same judge can be processed without causing a constraint violation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(backoff = @Backoff(delay = 50), retryFor = {DataIntegrityViolationException.class, PSQLException.class}, maxAttempts = 10)
    public JudgeEntity retrieveOrCreateJudge(String judgeName) {
        UserAccountEntity userAccount = authorisationApi.getCurrentUser();
        return judgeCommonService.retrieveOrCreateJudge(judgeName, userAccount);
    }

    /**
     * This method is only used to add judges during daily list processing. This task processed data in serial and therefore
     * there are no concurrent write concerns.
     */
    public JudgeEntity retrieveOrCreateJudge(String judgeName, UserAccountEntity userAccount) {
        return judgeCommonService.retrieveOrCreateJudge(judgeName, userAccount);
    }
}