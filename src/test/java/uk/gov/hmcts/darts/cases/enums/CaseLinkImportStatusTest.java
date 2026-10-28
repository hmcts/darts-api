package uk.gov.hmcts.darts.cases.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseLinkImportStatusTest {

    @ParameterizedTest
    @CsvSource({"1,RECEIVED", "2,PROCESSING", "3,COMPLETED", "4,COMPLETED_WITH_ERRORS", "5,REJECTED", "6,FAILED"})
    void given_lookupId_when_mappingStatus_then_returnApiStatus(Integer id, CaseLinkImportStatus expected) {
        assertThat(CaseLinkImportStatus.fromId(id)).isEqualTo(expected);
        assertThat(expected.getId()).isEqualTo(id);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 0, 7})
    void given_unknownLookupId_when_mappingStatus_then_reject(Integer id) {
        assertThatThrownBy(() -> CaseLinkImportStatus.fromId(id)).isInstanceOf(IllegalArgumentException.class);
    }
}
