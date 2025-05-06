package uk.gov.hmcts.darts.transcriptions.model;

import java.time.LocalDate;

public record TranscriptionDocumentResult(
    Long transcriptionDocumentId,
    Long transcriptionId,
    Integer caseId,
    String caseNumber,
    Integer hearingId,
    Integer hearingCaseId,
    String hearingCaseNumber,
    String courthouseDisplayName,
    String hearingCourthouseDisplayName,
    LocalDate hearingDate,
    boolean isManualTranscription,
    boolean isHidden
) {
}