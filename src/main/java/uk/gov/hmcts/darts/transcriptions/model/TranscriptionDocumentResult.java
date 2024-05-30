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
        return transcriptionDocumentResult.transcriptionDocumentId.equals(transcriptionDocumentId)
            && transcriptionDocumentResult.transcriptionId.equals(transcriptionId)
            && ((transcriptionDocumentResult.caseId != null && transcriptionDocumentResult.caseId.equals(caseId))
            || (transcriptionDocumentResult.caseId == null && caseId == null))
            && ((transcriptionDocumentResult.hearingId != null && transcriptionDocumentResult.hearingId.equals(hearingId))
            || (transcriptionDocumentResult.hearingId == null && hearingId == null))
            && ((transcriptionDocumentResult.courthouseId != null && transcriptionDocumentResult.courthouseId.equals(courthouseId))
            || (transcriptionDocumentResult.courthouseId == null && courthouseId == null));
    }

    @Override
    public int hashCode() {
        return Objects.hash(transcriptionId);
    }
}