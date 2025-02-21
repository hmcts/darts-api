package uk.gov.hmcts.darts.transcriptions.model;

import java.time.LocalDate;

public record TranscriptionDocumentResult(
    Integer transcriptionDocumentId,
    Integer transcriptionId,
    Integer caseId,
    String caseNumber,
    String hearingCaseNumber,
    String courthouseDisplayName,
    String hearingCourthouseDisplayName,
    LocalDate hearingDate,
    boolean isManualTranscription,
    boolean isHidden
) {
}