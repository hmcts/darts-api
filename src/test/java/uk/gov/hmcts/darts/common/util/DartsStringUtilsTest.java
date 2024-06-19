package uk.gov.hmcts.darts.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DartsStringUtilsTest {


    @ParameterizedTest
    @CsvSource({
        "string, STRING",
        "string with spaces, STRING_WITH_SPACES",
        "string with spaces and (brackets), STRING_WITH_SPACES_AND_(BRACKETS)"
    })
    void toScreamingSnakeCaseShouldTransformStringAsExpected(String input, String expected) {
        String actual = DartsStringUtils.toScreamingSnakeCase(input);
        assertEquals(expected, actual);
    }

}