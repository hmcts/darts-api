package uk.gov.hmcts.darts.cases.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import uk.gov.hmcts.darts.cases.service.CaseLinkCsvService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseLinkCsvServiceImplTest {

    private final CaseLinkCsvService service = new CaseLinkCsvServiceImpl();

    @DisplayName("""
        read()
        SHOULD skip the first CSV record without inspecting header names
            AND preserve supplied values and record numbers
            AND retain blank and incomplete rows for row validation
        WHEN the CSV has an arbitrary header and quoted fields
        """)
    @Test
    void read_shouldPreserveDataRows_whenHeaderAndFieldFormatsVary() {
        var rows = service.readCaseLinkCsvFile(file("ignored header\r\n\" X1 \", york ,CP1\r\n\r\nX2,YORK\r\n"));
        assertThat(rows).hasSize(3);
        assertThat(rows.getFirst().getRowNumber()).isEqualTo(2);
        assertThat(rows.getFirst().getCase1CaseNumber()).isEqualTo(" X1 ");
        assertThat(rows.getFirst().getCase1Courthouse()).isEqualTo(" york ");
        assertThat(rows.getFirst().getCase2Courthouse()).isEqualTo(" york ");
        assertThat(rows.get(1).getColumnCount()).isEqualTo(1);
        assertThat(rows.get(2).getCase2CaseNumber()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ignored header", ",,", "duplicate,duplicate,duplicate", ""})
    void read_shouldSkipFirstRecord_whenHeaderNamesAreMissingOrDuplicated(String header) {
        var caseLinkRows = service.readCaseLinkCsvFile(file(header + "\nX1,YORK,CP1\n"));

        assertThat(caseLinkRows).singleElement().satisfies(caseLinkRow -> {
            assertThat(caseLinkRow.getRowNumber()).isEqualTo(2);
            assertThat(caseLinkRow.getCase1CaseNumber()).isEqualTo("X1");
            assertThat(caseLinkRow.getCase1Courthouse()).isEqualTo("YORK");
            assertThat(caseLinkRow.getCase2CaseNumber()).isEqualTo("CP1");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "header\n", "header\nX1,YORK,\"unclosed", "header\nX1,YORK,\"closed\"garbage"})
    void read_shouldRejectFile_whenCsvIsEmptyOrMalformed(String content) {
        assertThatThrownBy(() -> service.readCaseLinkCsvFile(file(content))).isInstanceOf(DartsApiException.class);
    }

    @Test
    void read_shouldRejectFile_whenContentIsNotValidUtf8() {
        var file = new MockMultipartFile("file", "cases.csv", "text/csv", new byte[]{(byte) 0xc3, (byte) 0x28});
        assertThatThrownBy(() -> service.readCaseLinkCsvFile(file)).isInstanceOf(DartsApiException.class);
    }

    @Test
    void read_shouldReturnAllRows_whenRowCountExceedsTenThousand() {
        assertThat(service.readCaseLinkCsvFile(file("header\n" + "X1,YORK,CP1\n".repeat(10_001)))).hasSize(10_001);
    }

    private MockMultipartFile file(String content) {
        return new MockMultipartFile("file", "cases.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
