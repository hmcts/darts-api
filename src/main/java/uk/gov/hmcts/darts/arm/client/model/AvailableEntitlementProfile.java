package uk.gov.hmcts.darts.arm.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class AvailableEntitlementProfile {

    private List<Profiles> profiles;
    private String status;
    private boolean demoMode;
    private boolean isError;
    private Integer responseStatus;
    private List<ResponseStatusMessage> responseStatusMessages;

    @Data
    @Builder
    @Jacksonized
    public static class ResponseStatusMessage {
        private String message;
        private boolean isError;
    }

    @Data
    @Builder
    @Jacksonized
    public static class Profiles {
        private String profileId;
        private String profileName;
        private Integer entitlementCount;
    }

}
