package uk.gov.hmcts.darts.arm.client.model.rpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@ToString()
public class BaseRpoResponse {

    Integer itemsCount;
    Integer status;
    Boolean demoMode;
    Boolean isError;
    Integer responseStatus;
    List<ResponseStatusMessage> responseStatusMessages;
    String exception;
    String message;

}
