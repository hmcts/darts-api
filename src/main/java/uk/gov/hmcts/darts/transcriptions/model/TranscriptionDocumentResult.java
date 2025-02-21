package uk.gov.hmcts.darts.transcriptions.model;

import java.time.LocalDate;
import java.util.Objects;

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

    @Override
    public int hashCode() {
        return Objects.hash(transcriptionId);
    }
}