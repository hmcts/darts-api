package uk.gov.hmcts.darts.log.service;

import uk.gov.hmcts.darts.log.util.DailyListLogJobReport;

public interface DailyListLogJobService {
    void logJobReport(DailyListLogJobReport report);
}