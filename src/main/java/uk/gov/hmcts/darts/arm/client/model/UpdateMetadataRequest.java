package uk.gov.hmcts.darts.arm.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateMetadataRequest {

    @JsonProperty("UseGuidsForFields")
    private boolean useGuidsForFields;

    @JsonProperty("manifest")
    private Manifest manifest;

    @JsonProperty(value = "itemId", required = true)
    @NonNull
    private String itemId;

    @Data
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Manifest {
        @JsonProperty("event_date")
        private String eventDate;

        @JsonProperty("ret_conf_score")
        private Integer retConfScore;

        @JsonProperty("ret_conf_reason")
        private String retConfReason;
    }
}