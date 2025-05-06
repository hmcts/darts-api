package uk.gov.hmcts.darts.transcriptions.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TranscriptionSearchResult(
    Long id,
    Integer caseId,
    String caseNumber,
    Integer courthouseId,
    Integer hearingId,
    LocalDate hearingDate,
    OffsetDateTime requestedAt,
    Integer transcriptionStatusId,
    Boolean isManualTranscription,
    OffsetDateTime approvedAt
) {

}