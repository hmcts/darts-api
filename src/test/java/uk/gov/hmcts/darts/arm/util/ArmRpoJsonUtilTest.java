package uk.gov.hmcts.darts.arm.util;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmRpoJsonUtilTest {

    @Test
    void sanitiseValidJson() {
        // given
        String validJson = """
            {
              "filter": "1",
              "filterBy": {},
              "matterId": "12345",
              "usePaging": true,
              "rowsNumber": 10,
              "pageIndex": 0,
              "orderBy": "createdDate",
              "orderByAsc": false,
              "search": ""
            }
            """;

        // when
        String sanitisedJson = ArmRpoJsonUtil.sanitise(validJson);

        // then
        String expectedResult = "{\"filter\":\"1\",\"filterBy\":{},\"matterId\":\"12345\",\"usePaging\":true,\"rowsNumber\":10,\"pageIndex\":0,"
            + "\"orderBy\":\"createdDate\",\"orderByAsc\":false,\"search\":\"\"}";
        assertEquals(expectedResult, sanitisedJson);
    }

    @Test
    void sanitiseInvalidJson() {
        // given
        // missing closing brace
        String invalidJson = """
            {
              "filter": "1",
              "filterBy": {},
              "matterId": "12345",
              "usePaging": true,
              "rowsNumber": 10,
              "pageIndex": 0,
              "orderBy": "createdDate",
              "orderByAsc": false,
              "search": ""
            """;

        // when
        var exception = assertThrows(IllegalArgumentException.class, () -> ArmRpoJsonUtil.sanitise(invalidJson));

        // then
        assertEquals("Failed to serialise the templated json", exception.getMessage());
    }
}