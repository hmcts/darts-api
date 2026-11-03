package uk.gov.hmcts.darts.cases.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import uk.gov.hmcts.darts.cases.config.CaseLinkImportConfiguration;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseLinkFileValidatorTest {

    private final CaseLinkImportConfiguration configuration = new CaseLinkImportConfiguration();
    private final CaseLinkFileValidator validator = new CaseLinkFileValidator(configuration);
    private final byte[] content = "header\nX1,YORK,CP1".getBytes(StandardCharsets.UTF_8);

    @ParameterizedTest
    @ValueSource(strings = {"cases.csv", "cases.CSV", "cases.CsV"})
    void validate_shouldAcceptFile_whenExtensionIsCsvAndContentTypeIsTextCsv(String filename) {
        var file = new MockMultipartFile("file", filename, "text/csv", content);
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "application/csv", "application/vnd.ms-excel", "text/plain", "application/json", "application/octet-stream", "text/html"})
    void validate_shouldRejectFile_whenContentTypeIsNotTextCsv(String contentType) {
        var file = new MockMultipartFile("file", "cases.csv", contentType, content);
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.INVALID_CONTENT_TYPE));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "cases", "cases.txt", "cases.xls", "cases.xlsx"})
    void validate_shouldRejectFile_whenExtensionIsNotCsv(String filename) {
        var file = new MockMultipartFile("file", filename, "text/csv", content);
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.INVALID_FILE_TYPE));
    }

    @Test
    void validate_shouldRejectFile_whenFileIsEmpty() {
        var file = new MockMultipartFile("file", "cases.csv", "text/csv", new byte[0]);
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.EMPTY_FILE));
    }

    @Test
    void validate_shouldEnforceMaximumSize_whenFileIsAtOrAboveLimit() {
        var file = new MockMultipartFile("file", "cases.csv", "text/csv", content);
        configuration.setMaxFileSize(DataSize.ofBytes(file.getSize()));
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
        configuration.setMaxFileSize(DataSize.ofBytes(file.getSize() - 1));
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.FILE_TOO_LARGE));
    }
}
