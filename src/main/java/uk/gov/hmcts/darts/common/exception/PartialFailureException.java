package uk.gov.hmcts.darts.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

@Getter
public class PartialFailureException extends DartsApiException {

    PartialFailureException(DartsApiError error, String payload) {
        super(error);
        this.getCustomProperties().put("partial_failure", payload);
    }

    public static PartialFailureException getPartialPayloadJson(DartsApiError error, Object obj) {
        String errorProcessing = "";
        try {
            errorProcessing = new ObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException pe) {
            errorProcessing = "Error occurred marshalling payload";
        }

        return new PartialFailureException(error, errorProcessing);
    }
}
