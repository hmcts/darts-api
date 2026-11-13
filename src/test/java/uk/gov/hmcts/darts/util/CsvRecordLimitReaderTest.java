package uk.gov.hmcts.darts.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvRecordLimitReaderTest {

    @DisplayName("""
        read()
        SHOULD count raw characters within each logical CSV record
            AND reset the count for each record separator
            AND preserve quotes, escaped quotes, delimiters and quoted newlines
        WHEN records at the limit are read in small chunks across field boundaries
        """)
    @ParameterizedTest
    @MethodSource("csvRecords")
    void read_shouldResetLimitAtRecordBoundaries_whenRecordsContainQuotedFieldsAndNewlines(String record) throws IOException {
        for (String separator : new String[]{"\n", "\r", "\r\n"}) {
            String csvContent = record + separator + record + separator;
            try (var reader = limitedReader(csvContent, record.length())) {
                assertThat(readAll(reader, 2)).isEqualTo(csvContent);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("csvRecords")
    void read_shouldThrowSuppliedException_whenRecordExceedsLimit(String record) throws IOException {
        var limitFailure = new IllegalStateException("Record exceeds limit");
        try (var reader = new CsvRecordLimitReader(new StringReader(record), record.length() - 1, () -> limitFailure)) {
            assertThatThrownBy(() -> readAll(reader, 2)).isSameAs(limitFailure);
        }
    }

    @Test
    void read_shouldRejectQuotedMultilineRecord_whenCombinedPhysicalLinesExceedLimit() throws IOException {
        try (var reader = limitedReader("\"ab\ncd\"", 4)) {
            assertThatThrownBy(() -> readAll(reader, 1)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void read_shouldHonourBufferOffsets_whenCheckingRecordLength() throws IOException {
        try (var reader = limitedReader("abc\nx", 3)) {
            char[] buffer = new char[8];

            assertThat(reader.read(buffer, 2, 3)).isEqualTo(3);
            assertThat(new String(buffer, 2, 3)).isEqualTo("abc");
            assertThat(reader.read(buffer, 1, 3)).isEqualTo(2);
            assertThat(new String(buffer, 1, 2)).isEqualTo("\nx");
            assertThat(reader.read(buffer, 0, buffer.length)).isEqualTo(-1);
        }
    }

    @Test
    void skip_shouldEnforceRecordLimit_whenSkippedCharactersExceedLimit() throws IOException {
        try (var reader = limitedReader("abcdef", 3)) {
            assertThatThrownBy(() -> reader.skip(6)).isInstanceOf(IllegalStateException.class);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void constructor_shouldRejectLimit_whenMaximumRecordCharactersIsNotPositive(int maxRecordCharacters) {
        assertThatThrownBy(() -> limitedReader("abc", maxRecordCharacters)).isInstanceOf(IllegalArgumentException.class);
    }

    private CsvRecordLimitReader limitedReader(String content, int maxRecordCharacters) {
        return new CsvRecordLimitReader(new StringReader(content), maxRecordCharacters, () -> new IllegalStateException("Record exceeds limit"));
    }

    private String readAll(Reader reader, int bufferSize) throws IOException {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[bufferSize];
        int charactersRead = reader.read(buffer);
        while (charactersRead != -1) {
            content.append(buffer, 0, charactersRead);
            charactersRead = reader.read(buffer);
        }
        return content.toString();
    }

    private static Stream<String> csvRecords() {
        return Stream.of("a,b,c", ",,", "\"\"", "\"a,b\",c,d", "\"a\"\"b\",c,d", "\"a\nb\",c,d", "a,\"b\r\nc\",d",
                         "a,b,\"c\r\nd\"", "a\"b,c,d", "\"a\" ,b,c", "\"a\",,\"b\"");
    }
}
