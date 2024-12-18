package uk.gov.hmcts.darts.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;

public final class CsvFileUtil {
    private CsvFileUtil() {

    }


    public static Iterable<CSVRecord> readCsv(Reader reader) throws IOException {

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .build();

        return csvFormat.parse(reader);
    }
}
