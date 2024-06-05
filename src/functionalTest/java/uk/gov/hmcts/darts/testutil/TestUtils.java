package uk.gov.hmcts.darts.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class TestUtils {

    private TestUtils() {
    }

    /**
     * Create an object mapper with configuration per the main application.
     *
     * @return an object mapper instance
     */
    public static ObjectMapper createObjectMapper() {
        return new ObjectMapperConfig().objectMapper();
    }

}
