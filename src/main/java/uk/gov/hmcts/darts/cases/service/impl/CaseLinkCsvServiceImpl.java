package uk.gov.hmcts.darts.cases.service.impl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.service.CaseLinkCsvService;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultStatusEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.util.CsvFileUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CaseLinkCsvServiceImpl implements CaseLinkCsvService {

    private static final int COLUMN_COUNT = 3;
    private static final char NULL_CHARACTER = '\0';

    @Override
    public List<CaseLinkImportResultEntity> readCaseLinkCsvFile(MultipartFile file) {
        var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        var format = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(false).build();
        try (var reader = new InputStreamReader(file.getInputStream(), decoder);
             var parser = CsvFileUtil.readCsv(reader, format)) {

            List<CaseLinkImportResultEntity> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (record.getRecordNumber() == 1) {
                    continue; // The first CSV record is always skipped; header names are not inspected.
                }
                if (record.stream().anyMatch(value -> value.indexOf(NULL_CHARACTER) >= 0)) {
                    throw new DartsApiException(CaseApiError.MALFORMED_CSV);
                }
                rows.add(createCaseLinkImportResult(record));
            }
            if (rows.isEmpty()) {
                throw new DartsApiException(CaseApiError.EMPTY_FILE);
            }
            return rows;
        } catch (IOException | IllegalArgumentException | java.io.UncheckedIOException exception) {
            throw new DartsApiException(CaseApiError.MALFORMED_CSV, exception);
        }
    }

    private CaseLinkImportResultEntity createCaseLinkImportResult(CSVRecord record) {
        CaseLinkImportResultEntity result = new CaseLinkImportResultEntity();
        result.setRowNumber(Math.toIntExact(record.getRecordNumber()));
        result.setColumnCount(record.size());
        result.setCase1CaseNumber(record.get(0));
        result.setCase1Courthouse(record.size() > 1 ? record.get(1) : null);
        result.setCase2Courthouse(result.getCase1Courthouse());
        result.setCase2CaseNumber(record.size() >= COLUMN_COUNT ? record.get(2) : null);
        result.setStatus(new CaseLinkImportResultStatusEntity(CaseLinkResultStatus.PENDING.getId()));
        return result;
    }
}
