package uk.gov.hmcts.darts.log.service;

import uk.gov.hmcts.darts.log.util.LogJobReport;

public interface LogJobService {

    void logJobReport(LogJobReport report);
}