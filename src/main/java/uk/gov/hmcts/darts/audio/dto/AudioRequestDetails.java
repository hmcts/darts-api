package uk.gov.hmcts.darts.audio.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder()
public class AudioRequestDetails {
    String caseId;
    String emailAddress;
    LocalDateTime startTime;
    LocalDateTime endTime;
    String requestType;
}
