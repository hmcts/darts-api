package uk.gov.hmcts.darts.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtils {

    public String getEnvVarValue(String environmentVariable) {
        String value = System.getenv(environmentVariable);
        if (value == null) {
            throw new RuntimeException(String.format("Environment variable not set: %s", environmentVariable));
        }
        return value;
    }

    public String getEnvVarValue(String environmentVariable, String defaultValue) {
        String value = System.getenv(environmentVariable);

        return value != null ? value : defaultValue;
    }

}
