package uk.gov.hmcts.darts.transcriptions.model;

import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode
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