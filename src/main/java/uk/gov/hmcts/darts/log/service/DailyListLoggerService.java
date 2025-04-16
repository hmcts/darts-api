package uk.gov.hmcts.darts.log.service;

import uk.gov.hmcts.darts.log.util.DailyListLogJobReport;

@FunctionalInterface
public interface DailyListLoggerService {
    void logJobReport(DailyListLogJobReport report);
}