package uk.gov.hmcts.darts.arm.client.model.rpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@ToString()
public class ResponseStatusMessage {
    String message;
    Boolean isError;
    Boolean canBeShownOnClient;
}
