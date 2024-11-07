package uk.gov.hmcts.darts.arm.component.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("checkstyle:LineLength")
class AddAsyncSearchRequestGeneratorTest {

    @Test
    void getJsonRequest_shouldReturnExpectedJsonString_whenProvidedWithTypicalInputValues() {
        // Given
        var addSyncSearchRequestGenerator = AddAsyncSearchRequestGenerator.builder()
            .name("name")
            .searchName("searchName")
            .matterId("matterId")
            .entitlementId("entitlementId")
            .indexId("indexId")
            .sortingField("sortingField")
            .startTime(OffsetDateTime.parse("2024-08-12T23:00:00.000Z"))
            .endTime(OffsetDateTime.parse("2024-08-13T23:00:00.000Z"))
            .build();

        // When
        String jsonRequest = addSyncSearchRequestGenerator.getJsonRequest();

        // Then
        assertEquals("""
                         {"queryTree":{"operator":1,"children":[{"field":{"dataScopeEntitlementNotApplied":true,"removable":false,"draggable":false,"fixedField":true,"history":false,"error":false,"name":"record_class","valueType":32,"value":["DARTS"]}},{"field":{"dataScopeEntitlementNotApplied":false,"removable":true,"draggable":true,"fixedField":false,"history":false,"error":false,"valueType":7,"name":"ingestionDate","value":["2024-08-12T23:00:00Z","2024-08-13T23:00:00Z"]}}]},"queryTreeWithin":{"operator":1,"children":[{"field":{"dataScopeEntitlementNotApplied":false,"removable":true,"draggable":true,"fixedField":false,"history":false,"error":false,"valueType":-1,"name":"","value":[]}}]},"queryFields":{},"clientTimezone":"Europe/London","timezone":"Europe/London","matterId":"matterId","entitlementId":"entitlementId","indexId":"indexId","sortingField":"sortingField","sortingType":1,"name":"name","searchName":"searchName"}""",
                     jsonRequest);
    }

    @Test
    void getJsonRequest_shouldReturnExpectedJsonString_whenProvidedWithEmptyStrings() {
        // Given
        var addSyncSearchRequestGenerator = AddAsyncSearchRequestGenerator.builder()
            .name("")
            .searchName("")
            .matterId("")
            .entitlementId("")
            .indexId("")
            .sortingField("")
            .startTime(OffsetDateTime.parse("2024-08-12T23:00:00.000Z"))
            .endTime(OffsetDateTime.parse("2024-08-13T23:00:00.000Z"))
            .build();

        // When
        String jsonRequest = addSyncSearchRequestGenerator.getJsonRequest();

        // Then
        assertEquals("""
                         {"queryTree":{"operator":1,"children":[{"field":{"dataScopeEntitlementNotApplied":true,"removable":false,"draggable":false,"fixedField":true,"history":false,"error":false,"name":"record_class","valueType":32,"value":["DARTS"]}},{"field":{"dataScopeEntitlementNotApplied":false,"removable":true,"draggable":true,"fixedField":false,"history":false,"error":false,"valueType":7,"name":"ingestionDate","value":["2024-08-12T23:00:00Z","2024-08-13T23:00:00Z"]}}]},"queryTreeWithin":{"operator":1,"children":[{"field":{"dataScopeEntitlementNotApplied":false,"removable":true,"draggable":true,"fixedField":false,"history":false,"error":false,"valueType":-1,"name":"","value":[]}}]},"queryFields":{},"clientTimezone":"Europe/London","timezone":"Europe/London","matterId":"","entitlementId":"","indexId":"","sortingField":"","sortingType":1,"name":"","searchName":""}""",
                     jsonRequest);
    }

    @ParameterizedTest
    @MethodSource("alternatingNulls")
    void objectConstruction_shouldThrowException_whenProvidedWithAnyNullValue(String name,
                                                                              String searchName,
                                                                              String matterId,
                                                                              String entitlementId,
                                                                              String indexId,
                                                                              String sortingField,
                                                                              OffsetDateTime startTime,
                                                                              OffsetDateTime endTime) {
        // When
        NullPointerException exception = assertThrows(NullPointerException.class, () -> AddAsyncSearchRequestGenerator.builder()
            .name(name)
            .searchName(searchName)
            .matterId(matterId)
            .entitlementId(entitlementId)
            .indexId(indexId)
            .sortingField(sortingField)
            .startTime(startTime)
            .endTime(endTime)
            .build());

        // Then
        assertThat(exception.getMessage(), containsString("is marked non-null but is null"));
    }

    private static Stream<Arguments> alternatingNulls() {
        String someString = "";
        OffsetDateTime someDateTime = OffsetDateTime.now();
        return Stream.of(
            Arguments.of(null, someString, someString, someString, someString, someString, someDateTime, someDateTime),
            Arguments.of(someString, null, someString, someString, someString, someString, someDateTime, someDateTime),
            Arguments.of(someString, someString, null, someString, someString, someString, someDateTime, someDateTime),
            Arguments.of(someString, someString, someString, null, someString, someString, someDateTime, someDateTime),
            Arguments.of(someString, someString, someString, someString, null, someString, someDateTime, someDateTime),
            Arguments.of(someString, someString, someString, someString, someString, null, someDateTime, someDateTime),
            Arguments.of(someString, someString, someString, someString, someString, someString, null, someDateTime),
            Arguments.of(someString, someString, someString, someString, someString, someString, someDateTime, null)
        );
    }

}
