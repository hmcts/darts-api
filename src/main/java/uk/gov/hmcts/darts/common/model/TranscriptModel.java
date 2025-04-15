package uk.gov.hmcts.darts.common.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface TranscriptModel {
    Integer getTranscriptionId();

    void setTranscriptionId(Integer transcriptionId);

    Integer getHearingId();

    void setHearingId(Integer hearingId);

    LocalDate getHearingDate();

    void setHearingDate(LocalDate hearingDate);

    String getType();

    void setType(String type);

    void setRequestedByName(String requestedByName);

    OffsetDateTime getRequestedOn();

    void setRequestedOn(OffsetDateTime requestedOn);

    String getStatus();

    void setStatus(String status);
}