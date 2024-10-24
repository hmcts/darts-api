package uk.gov.hmcts.darts.arm.model.rpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class ResponseStatusMessage {
    String message;
    Boolean isError;
}
