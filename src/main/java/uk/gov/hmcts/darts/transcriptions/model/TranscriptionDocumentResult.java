package uk.gov.hmcts.darts.transcriptions.model;

import java.time.LocalDate;
import java.util.Objects;

public record TranscriptionDocumentResult(
    Integer transcriptionDocumentId,
    Integer transcriptionId,
    Integer caseId,
    String caseNumber,
    Integer courthouseId,
    String courthouseDisplayName,
    Integer hearingCourthouseId,
    String hearingCourthouseDisplayName,
    Integer hearingId,
    LocalDate hearingDate,
    boolean isManualTranscription,
    boolean isHidden
) {

    @SuppressWarnings({"PMD.SimplifyBooleanReturns"})
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof TranscriptionDocumentResult transcriptionDocumentResult)) {
            return false;
        }
        return transcriptionDocumentResult.transcriptionId.equals(this.transcriptionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transcriptionId);
    }
}