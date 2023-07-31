package uk.gov.hmcts.darts.common.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.time.LocalDate;

public interface RetrieveCoreObjectService {

    @Transactional
    @Retryable(backoff = @Backoff(delay = 50), retryFor = DataIntegrityViolationException.class)
    HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDate hearingDate);

    @Transactional
    @Retryable(backoff = @Backoff(delay = 50), retryFor = DataIntegrityViolationException.class)
    CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName);

    @Transactional
    @Retryable(backoff = @Backoff(delay = 50), retryFor = DataIntegrityViolationException.class)
    CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName);

    @Transactional
    @Retryable(backoff = @Backoff(delay = 50), retryFor = DataIntegrityViolationException.class)
    CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber);

    @Retryable(backoff = @Backoff(delay = 50), retryFor = DataIntegrityViolationException.class)
    CourthouseEntity retrieveCourthouse(String courthouseName);
}
