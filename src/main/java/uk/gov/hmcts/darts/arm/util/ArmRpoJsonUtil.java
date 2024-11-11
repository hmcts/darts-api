package uk.gov.hmcts.darts.arm.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class ArmRpoJsonUtil {

    /**
     * Strip the provided JSON string of excess whitespace/newlines to make it more suitable for an HTTP request.
     */
    public static String sanitise(String json) {
        JsonNode jsonNode;
        try {
            jsonNode = new ObjectMapper().readTree(json);
        } catch (JsonProcessingException e) {
            // This should never happen as we're in control of the template.
            // If it does fail, assume it's due to some wacky argument provided upon construction.
            throw new IllegalArgumentException("Failed to serialise the templated json", e);
        }
        return jsonNode.toString();
    }
}
