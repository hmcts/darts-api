package uk.gov.hmcts.darts.arm.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.arm.util.ArmRpoJsonUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetExtendedSearchesByMatterRequestGeneratorTest {

    private GetExtendedSearchesByMatterRequestGenerator generator;

    @BeforeEach
    void setUp() {
        generator = GetExtendedSearchesByMatterRequestGenerator.builder()
            .matterId("12345")
            .build();
    }

    @Test
    void getJsonRequest() {
        String expectedJson = """
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
        String actualJson = generator.getJsonRequest();
        assertEquals(ArmRpoJsonUtil.sanitise(expectedJson), actualJson);
    }

    @Test
    void getJsonRequestWithNullMatterIdThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            GetExtendedSearchesByMatterRequestGenerator.builder()
                .matterId(null)
                .build()
                .getJsonRequest();
        });
    }
}
