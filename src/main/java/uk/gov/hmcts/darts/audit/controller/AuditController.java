package uk.gov.hmcts.darts.audit.controller;

import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.darts.event.api.AuditApi;
import uk.gov.hmcts.darts.event.model.SearchResult;

import java.time.OffsetDateTime;

public class AuditController implements AuditApi {
    @Override
    public ResponseEntity<SearchResult> search(Object startDate, OffsetDateTime endDate, String caseId, String eventId) {
        return AuditApi.super.search(startDate, endDate, caseId, eventId);
    }
}
