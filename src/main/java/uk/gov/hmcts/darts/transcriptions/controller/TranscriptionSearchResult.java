package uk.gov.hmcts.darts.transcriptions.controller;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TranscriptionSearchResult(
    Integer id,
    String caseNumber,
    Integer courthouseId,
    LocalDate hearingDate,
    OffsetDateTime requestedAt,
    Integer transcriptionStatusId,
    Boolean isManualTranscription
) {
}