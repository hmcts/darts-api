package uk.gov.hmcts.darts.event.service;


import uk.gov.hmcts.darts.event.model.CourtLog;

import java.time.OffsetDateTime;
import java.util.List;

@FunctionalInterface
public interface CourtLogsService {
    List<CourtLog> getCourtLogs(String courtHouse, String caseNumber, OffsetDateTime start, OffsetDateTime end);
}
