package uk.gov.hmcts.darts.util;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvFileUtilTest {

    @Test
    void readCsv_shouldReadNamedColumnsAndSkipBlankLines_whenUsingDefaultFormat() throws IOException {
        try (var reader = new StringReader("ClientIdentifier,Status\n123,Ready\n\n456,Complete\n")) {
            List<CSVRecord> csvRecords = new ArrayList<>();
            CsvFileUtil.readCsv(reader).forEach(csvRecords::add);

            assertThat(csvRecords).extracting(csvRecord -> csvRecord.get("ClientIdentifier")).containsExactly("123", "456");
            assertThat(csvRecords).extracting(csvRecord -> csvRecord.get("Status")).containsExactly("Ready", "Complete");
        }
    }
}
