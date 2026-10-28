package uk.gov.hmcts.darts.cases.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import uk.gov.hmcts.darts.cases.config.CaseLinkImportConfiguration;
import uk.gov.hmcts.darts.cases.exception.CaseLinkRowException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseLinkCsvServiceTest {

    private final CaseLinkImportConfiguration configuration = new CaseLinkImportConfiguration();
    private final CaseLinkCsvService service = new CaseLinkCsvService(configuration);

    @Test
    void given_arbitraryHeaderAndQuotedFields_when_reading_then_skipHeaderAndPreserveSuppliedDetails() {
        var rows = service.read(file("ignored header\r\n\" X1 \", york ,CP1\r\n\r\nX2,YORK\r\n"));
        assertThat(rows).hasSize(3);
        assertThat(rows.getFirst().getRowNumber()).isEqualTo(2);
        assertThat(rows.getFirst().getCase1CaseNumber()).isEqualTo(" X1 ");
        assertThat(rows.getFirst().getCase1Courthouse()).isEqualTo(" york ");
        assertThat(rows.getFirst().getCase2Courthouse()).isEqualTo(" york ");
        assertThat(rows.get(1).getColumnCount()).isEqualTo(1);
        assertThat(rows.get(2).getCase2CaseNumber()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "header\n", "header\nX1,YORK,\"unclosed", "header\nX1,YORK,\"closed\"garbage"})
    void given_emptyOrMalformedCsv_when_reading_then_rejectFile(String content) {
        assertThatThrownBy(() -> service.read(file(content))).isInstanceOf(CaseLinkRowException.class);
    }

    @Test
    void given_invalidUtf8_when_reading_then_rejectFile() {
        var file = new MockMultipartFile("file", "cases.csv", "text/csv", new byte[]{(byte) 0xc3, (byte) 0x28});
        assertThatThrownBy(() -> service.read(file)).isInstanceOf(CaseLinkRowException.class);
    }

    @Test
    void given_manyRowsWithinFileSize_when_reading_then_acceptWithoutRowLimit() {
        assertThat(service.read(file("header\n" + "X1,YORK,CP1\n".repeat(10_001)))).hasSize(10_001);
    }

    @Test
    void given_sizeLimit_when_reading_then_acceptBoundaryAndRejectExcess() {
        var file = file("header\nX1,YORK,CP1");
        configuration.setMaxFileSize(DataSize.ofBytes(file.getSize()));
        assertThat(service.read(file)).hasSize(1);
        configuration.setMaxFileSize(DataSize.ofBytes(file.getSize() - 1));
        assertThatThrownBy(() -> service.read(file))
            .isInstanceOfSatisfying(CaseLinkRowException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo("FILE_TOO_LARGE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/octet-stream", "text/html"})
    void given_invalidContentType_when_reading_then_rejectFile(String contentType) {
        var file = new MockMultipartFile("file", "cases.csv", contentType, "header\nX1,YORK,CP1".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> service.read(file)).isInstanceOf(CaseLinkRowException.class);
    }

    @Test
    void given_invalidExtension_when_reading_then_rejectFile() {
        var file = new MockMultipartFile("file", "cases.txt", "text/csv", "header\nX1,YORK,CP1".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> service.read(file)).isInstanceOf(CaseLinkRowException.class);
    }

    private MockMultipartFile file(String content) {
        return new MockMultipartFile("file", "cases.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
