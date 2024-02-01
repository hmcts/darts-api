package uk.gov.hmcts.darts.arm.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateMetadataResponse {

    private UUID itemId;
    private Integer cabinetId;
    private UUID objectId;
    private Integer objectType;
    private String fileName;
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

}
