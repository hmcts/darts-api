package uk.gov.hmcts.darts.common.service;

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
    @Retryable(backoff = @Backoff(delay = 50))
    HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDate hearingDate);

    @Transactional
    @Retryable(backoff = @Backoff(delay = 50))
    CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName);

    @Transactional
    @Retryable(backoff = @Backoff(delay = 50))
    CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName);

    @Transactional
    @Retryable(backoff = @Backoff(delay = 50))
    CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber);

    @Retryable(backoff = @Backoff(delay = 50))
    CourthouseEntity retrieveCourthouse(String courthouseName);
}
