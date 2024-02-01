package uk.gov.hmcts.darts.arm.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;

@Data
@Builder
@Jacksonized
public class UpdateMetadataRequest {

    @JsonProperty("UseGuidsForFields")
    private boolean useGuidsForFields;

    @JsonProperty("manifest")
    private Manifest manifest;

    @JsonProperty("itemId")
    private String itemId;

    @Data
    @Builder
    @Jacksonized
    public static class Manifest {
        @JsonProperty("event_date")
        private OffsetDateTime eventDate;
    }

}
