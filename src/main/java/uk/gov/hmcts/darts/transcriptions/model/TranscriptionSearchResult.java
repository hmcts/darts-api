package uk.gov.hmcts.darts.transcriptions.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

public record TranscriptionSearchResult(
    Integer id,
    String caseNumber,
    Integer courthouseId,
    LocalDate hearingDate,
    OffsetDateTime requestedAt,
    Integer transcriptionStatusId,
    Boolean isManualTranscription,
    OffsetDateTime approvedAt
) {

    @Override
    @SuppressWarnings({"PMD.SimplifyBooleanReturns"})
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof TranscriptionSearchResult transcriptionSearchResult)) {
            return false;
        }
        return transcriptionSearchResult.id.equals(this.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}