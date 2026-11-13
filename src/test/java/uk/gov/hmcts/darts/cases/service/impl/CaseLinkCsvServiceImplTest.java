package uk.gov.hmcts.darts.cases.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import uk.gov.hmcts.darts.cases.config.CaseLinkImportConfiguration;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.service.CaseLinkCsvService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaseLinkCsvServiceImplTest {

    private final CaseLinkImportConfiguration configuration = new CaseLinkImportConfiguration();
    private final CaseLinkCsvService service = new CaseLinkCsvServiceImpl(configuration);

    @DisplayName("""
        readCaseLinkCsvFile()
        SHOULD skip the first CSV record without inspecting header names
            AND preserve supplied values and record numbers
            AND retain blank and incomplete rows for row validation
        WHEN the CSV has an arbitrary header and quoted fields
        """)
    @Test
    void readCaseLinkCsvFile_shouldPreserveDataRows_whenHeaderAndFieldFormatsVary() {
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
    void readCaseLinkCsvFile_shouldSkipFirstRecord_whenHeaderNamesAreMissingOrDuplicated(String header) {
        var caseLinkRows = service.readCaseLinkCsvFile(file(header + "\nX1,YORK,CP1\n"));

        assertThat(caseLinkRows).singleElement().satisfies(caseLinkRow -> {
            assertThat(caseLinkRow.getRowNumber()).isEqualTo(2);
            assertThat(caseLinkRow.getCase1CaseNumber()).isEqualTo("X1");
            assertThat(caseLinkRow.getCase1Courthouse()).isEqualTo("YORK");
            assertThat(caseLinkRow.getCase2CaseNumber()).isEqualTo("CP1");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "header\n"})
    void readCaseLinkCsvFile_shouldRejectFileAsEmpty_whenCsvHasNoDataRows(String content) {
        assertThatThrownBy(() -> service.readCaseLinkCsvFile(file(content)))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.EMPTY_FILE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"header\nX1,YORK,\"unclosed", "header\nX1,YORK,\"closed\"garbage"})
    void readCaseLinkCsvFile_shouldRejectFileAsMalformed_whenCsvSyntaxIsInvalid(String content) {
        assertThatThrownBy(() -> service.readCaseLinkCsvFile(file(content)))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> {
                assertThat(exception.getError()).isEqualTo(CaseApiError.MALFORMED_CSV);
                assertThat(exception.getCause()).isInstanceOf(UncheckedIOException.class);
            });
    }

    @Test
    void readCaseLinkCsvFile_shouldRejectFile_whenContentIsNotValidUtf8() {
        var file = new MockMultipartFile("file", "cases.csv", "text/csv", new byte[]{(byte) 0xc3, (byte) 0x28});
        assertThatThrownBy(() -> service.readCaseLinkCsvFile(file))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> {
                assertThat(exception.getError()).isEqualTo(CaseApiError.MALFORMED_CSV);
                assertThat(exception).hasRootCauseInstanceOf(CharacterCodingException.class);
            });
    }

    @Test
    void readCaseLinkCsvFile_shouldPreserveFileReadFailure_whenUploadStreamCannotBeOpened() throws IOException {
        var uploadedFile = spy(file("header\nX1,YORK,CP1\n"));
        IOException openingFailure = new IOException("Upload stream unavailable");
        doThrow(openingFailure).when(uploadedFile).getInputStream();

        assertThatThrownBy(() -> service.readCaseLinkCsvFile(uploadedFile))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> {
                assertThat(exception.getError()).isEqualTo(CaseApiError.FILE_READ_FAILED);
                assertThat(exception.getCause()).isSameAs(openingFailure);
            });
    }

    @Test
    void readCaseLinkCsvFile_shouldPreserveWrappedFileReadFailure_whenUploadStreamReadFails() throws IOException {
        var uploadedFile = spy(file("header\nX1,YORK,CP1\n"));
        try (InputStream failingInputStream = mock(InputStream.class)) {
            IOException readingFailure = new IOException("Upload stream read failed");
            doReturn(failingInputStream).when(uploadedFile).getInputStream();
            when(failingInputStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(readingFailure);

            assertThatThrownBy(() -> service.readCaseLinkCsvFile(uploadedFile))
                .isInstanceOfSatisfying(DartsApiException.class, exception -> {
                    assertThat(exception.getError()).isEqualTo(CaseApiError.FILE_READ_FAILED);
                    assertThat(exception.getCause()).isInstanceOf(UncheckedIOException.class).hasCause(readingFailure);
                });

            verify(failingInputStream).close();
        }
    }

    @Test
    void readCaseLinkCsvFile_shouldPreserveFileReadFailure_whenUploadStreamCannotBeClosed() throws IOException {
        byte[] csvContent = "header\nX1,YORK,CP1\n".getBytes(StandardCharsets.UTF_8);
        var uploadedFile = spy(file("header\nX1,YORK,CP1\n"));
        InputStream failingInputStream = spy(new ByteArrayInputStream(csvContent));
        IOException closingFailure = new IOException("Upload stream close failed");
        doReturn(failingInputStream).when(uploadedFile).getInputStream();
        doThrow(closingFailure).when(failingInputStream).close();

        assertThatThrownBy(() -> service.readCaseLinkCsvFile(uploadedFile))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> {
                assertThat(exception.getError()).isEqualTo(CaseApiError.FILE_READ_FAILED);
                assertThat(exception.getCause()).isSameAs(closingFailure);
            });
    }

    @Test
    void readCaseLinkCsvFile_shouldRejectFile_whenDataRecordContainsNullCharacter() {
        assertThatThrownBy(() -> service.readCaseLinkCsvFile(file("header\nX1,YORK,CP1" + Character.MIN_VALUE)))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.MALFORMED_CSV));
    }

    @Test
    void readCaseLinkCsvFile_shouldAcceptRows_whenDataRowCountEqualsDefaultMaximum() {
        var caseLinkRows = service.readCaseLinkCsvFile(file("header\n" + "X1,YORK,CP1\n".repeat(100_000)));

        assertThat(caseLinkRows).hasSize(100_000);
        assertThat(caseLinkRows.getLast().getRowNumber()).isEqualTo(100_001);
    }

    @ParameterizedTest
    @ValueSource(strings = {"X1,YORK,CP1\n", "\n"})
    void readCaseLinkCsvFile_shouldRejectFile_whenDataRowCountExceedsDefaultMaximum(String dataRow) {
        assertThatThrownBy(() -> service.readCaseLinkCsvFile(file("header\n" + dataRow.repeat(100_001))))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.TOO_MANY_ROWS));
    }

    @Test
    void readCaseLinkCsvFile_shouldEnforceConfiguredLimit_whenBlankRowsArePresent() {
        configuration.setMaxRows(2);
        String csvContent = "header\nX1,YORK,CP1\n\n";

        assertThat(service.readCaseLinkCsvFile(file(csvContent))).hasSize(2);
        assertThatThrownBy(() -> service.readCaseLinkCsvFile(file(csvContent + "X2,YORK,CP2\n")))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.TOO_MANY_ROWS));
    }

    @Test
    void readCaseLinkCsvFile_shouldRejectBeforeParsingRemainingRecords_whenConfiguredLimitIsExceeded() {
        configuration.setMaxRows(1);

        assertThatThrownBy(() -> service.readCaseLinkCsvFile(file("header\nX1,YORK,CP1\nX2,YORK,CP2\n\"unclosed")))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.TOO_MANY_ROWS));
    }

    @Test
    void readCaseLinkCsvFile_shouldRejectRecordBeforeReadingWholeStream_whenColumnCountIsExtreme() throws IOException {
        byte[] csvContent = ("header\n" + "a,".repeat(1_000_000) + "\n").getBytes(StandardCharsets.UTF_8);
        var uploadedFile = spy(new MockMultipartFile("file", "cases.csv", "text/csv", csvContent));
        var csvInputStream = new ByteArrayInputStream(csvContent);
        doReturn(csvInputStream).when(uploadedFile).getInputStream();

        assertThatThrownBy(() -> service.readCaseLinkCsvFile(uploadedFile))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.CSV_RECORD_TOO_LARGE));

        assertThat(csvInputStream.available()).isGreaterThan(csvContent.length / 2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r", "\r\n"})
    void readCaseLinkCsvFile_shouldAcceptRecordAtLimit_whenConfiguredLimitMatchesRawRecordLength(String recordSeparator) {
        configuration.setMaxRecordCharacters("X1,YORK,CP1".length());

        var caseLinkRows = service.readCaseLinkCsvFile(file("header" + recordSeparator + "X1,YORK,CP1" + recordSeparator));

        assertThat(caseLinkRows).hasSize(1);
        assertThat(caseLinkRows.getFirst().getCase2CaseNumber()).isEqualTo("CP1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"header\nX1,YORK,CP1\n", "header\n\"a\nb\nc\",YORK,CP1\n", "oversized header\nX1,YORK,CP1\n"})
    void readCaseLinkCsvFile_shouldRejectWholeFile_whenDataOrHeaderRecordExceedsConfiguredLimit(String csvContent) {
        configuration.setMaxRecordCharacters(10);

        assertThatThrownBy(() -> service.readCaseLinkCsvFile(file(csvContent)))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.CSV_RECORD_TOO_LARGE));
    }

    private MockMultipartFile file(String content) {
        return new MockMultipartFile("file", "cases.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
