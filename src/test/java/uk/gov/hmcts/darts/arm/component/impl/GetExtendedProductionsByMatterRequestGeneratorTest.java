package uk.gov.hmcts.darts.arm.component.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.arm.util.ArmRpoJsonUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetExtendedProductionsByMatterRequestGeneratorTest {

    private GetExtendedProductionsByMatterRequestGenerator generator;

    @BeforeEach
    void setUp() {
        generator = GetExtendedProductionsByMatterRequestGenerator.builder()
            .matterId("12345")
            .build();
    }

    @Test
    void getJsonRequest() {
        String expectedJson = """
        {
          "filterBy": {},
          "filter": "1",
          "matterId": "12345",
          "usePaging": true,
          "rowsNumber": 10,
          "pageIndex": 0,
          "orderBy": "startProductionTime",
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
            GetExtendedProductionsByMatterRequestGenerator.builder()
                .matterId(null)
                .build()
                .getJsonRequest();
        });
    }
}
