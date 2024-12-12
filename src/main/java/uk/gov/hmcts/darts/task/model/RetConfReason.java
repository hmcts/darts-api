package uk.gov.hmcts.darts.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class RetConfReason {
    @JsonProperty("manual_deletion_ts")
    OffsetDateTime manualDeletionTs;
    @JsonProperty("ret_conf_reason")
    String manualDeletionReason;
    @JsonProperty("ticket_reference")
    String ticketReference;
    @JsonProperty("comments")
    String comments;
}