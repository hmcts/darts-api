package uk.gov.hmcts.darts.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@Jacksonized
public class DarNotifyEvent {

    @JsonProperty("notification_url")
    private String notificationUrl;
    @JsonProperty("notification_type")
    private String notificationType;
    @JsonProperty("timestamp")
    private OffsetDateTime timestamp;
    @JsonProperty("courthouse")
    private String courthouse;
    @JsonProperty("courtroom")
    private String courtroom;
    @JsonProperty("case_numbers")
    private List<String> caseNumbers;

}
