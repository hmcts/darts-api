package uk.gov.hmcts.darts.event.service;


import uk.gov.hmcts.darts.events.model.CourtLog;

import java.time.OffsetDateTime;
import java.util.List;

public interface CourtLogsService {

    List<CourtLog> getCourtLogs(String courtHouse, String caseNumber, OffsetDateTime start, OffsetDateTime end);

}
