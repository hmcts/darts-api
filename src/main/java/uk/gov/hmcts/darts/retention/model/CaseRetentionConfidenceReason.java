package uk.gov.hmcts.darts.retention.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseRetentionConfidenceReason {
    @JsonProperty("ret_conf_applied_ts")
    private String retentionConfidenceAppliedTimestamp;

    @JsonProperty("cases")
    private List<RetentionCase> retentionCases;

    @Data
    @Builder
    @Jacksonized
    public static class RetentionCase {
        @JsonProperty("courthouse")
        private String courthouse;
        @JsonProperty("case_number")
        private String caseNumber;
        @JsonProperty("ret_conf_updated_ts")
        private String retentionConfidenceUpdatedTimestamp;
        @JsonProperty("ret_conf_reason")
        private String retentionConfidenceReason;
    }
}
