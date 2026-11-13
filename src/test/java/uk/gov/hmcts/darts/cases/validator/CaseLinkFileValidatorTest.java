package uk.gov.hmcts.darts.cases.validator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.mock.web.MockPart;
import org.springframework.util.unit.DataSize;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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
    private RequestAttributes originalRequestAttributes;

    @BeforeEach
    void setUp() {
        originalRequestAttributes = RequestContextHolder.getRequestAttributes();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.setRequestAttributes(originalRequestAttributes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cases.csv", "cases.CSV", "cases.CsV"})
    void validate_shouldAcceptFile_whenExtensionIsCsvAndContentTypeIsTextCsv(String filename) {
        var file = new MockMultipartFile("file", filename, "text/csv", content);
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Text/CSV",
        "TEXT/CSV",
        "text/csv; charset=UTF-8",
        "Text/CSV; charset=UTF-8",
        "text/csv ; charset=UTF-8"
    })
    void validate_shouldAcceptFile_whenCsvContentTypeUsesDifferentCasingOrParameters(String contentType) {
        var uploadedFile = new MockMultipartFile("file", "cases.csv", contentType, content);

        assertThatCode(() -> validator.validate(uploadedFile)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "", "application/csv", "application/vnd.ms-excel", "text/plain", "application/json", "application/octet-stream", "text/html",
        "text/plain; charset=UTF-8", "application/csv; charset=UTF-8"
    })
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

    @ParameterizedTest
    @ValueSource(strings = {"file", "extra_file"})
    void validate_shouldRejectFileCount_whenRequestContainsAdditionalUploadedFile(String extraPartName) {
        var uploadedFile = new MockMultipartFile("file", "cases.csv", "text/csv", content);
        var multipartRequest = new MockMultipartHttpServletRequest();
        multipartRequest.addFile(uploadedFile);
        multipartRequest.addFile(new MockMultipartFile(extraPartName, "extra.csv", "text/csv", content));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(multipartRequest));

        assertThatThrownBy(() -> validator.validate(uploadedFile))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.INVALID_FILE_COUNT));
    }

    @Test
    void validate_shouldAcceptFile_whenMultipartRequestContainsExactlyOneUploadedFile() {
        var uploadedFile = new MockMultipartFile("file", "cases.csv", "text/csv", content);
        var multipartRequest = new MockMultipartHttpServletRequest();
        multipartRequest.addFile(uploadedFile);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(multipartRequest));

        assertThatCode(() -> validator.validate(uploadedFile)).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectFileCount_whenMultipartRequestContainsNoUploadedFiles() {
        var uploadedFile = new MockMultipartFile("file", "cases.csv", "text/csv", content);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockMultipartHttpServletRequest()));

        assertThatThrownBy(() -> validator.validate(uploadedFile))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.INVALID_FILE_COUNT));
    }

    @Test
    void validate_shouldAcceptProgrammaticFile_whenRequestIsNotMultipart() {
        var uploadedFile = new MockMultipartFile("file", "cases.csv", "text/csv", content);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        assertThatCode(() -> validator.validate(uploadedFile)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"file", "extra_file"})
    void validate_shouldRejectFileCount_whenServletRequestHasAdditionalFileParts(String extraPartName) {
        var uploadedFile = new MockMultipartFile("file", "cases.csv", "text/csv", content);
        var servletRequest = new MockHttpServletRequest();
        servletRequest.setContentType("multipart/form-data");
        servletRequest.addPart(new MockPart("file", "cases.csv", content));
        servletRequest.addPart(new MockPart(extraPartName, "extra.csv", content));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

        assertThatThrownBy(() -> validator.validate(uploadedFile))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.INVALID_FILE_COUNT));
    }
}
