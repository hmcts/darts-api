package uk.gov.hmcts.darts.cases.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.cases.config.CaseLinkImportConfiguration;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.exception.CaseLinkRowException;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultStatusEntity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CaseLinkCsvService {

    private static final int COLUMN_COUNT = 3;
    private static final Set<String> CONTENT_TYPES = Set.of("text/csv", "application/csv", "application/vnd.ms-excel", "text/plain");
    private final CaseLinkImportConfiguration configuration;

    public List<CaseLinkImportResultEntity> read(MultipartFile file) {
        validateFile(file);
        var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        var format = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(false).build();
        try (var reader = new InputStreamReader(file.getInputStream(), decoder);
             var parser = format.parse(reader)) {
            List<CaseLinkImportResultEntity> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (record.getRecordNumber() == 1) {
                    continue; // The first CSV record is always skipped; header names are not inspected.
                }
                if (record.stream().anyMatch(value -> value.indexOf('\0') >= 0)) {
                    throw new CaseLinkRowException("MALFORMED_CSV", "The CSV contains a binary null character");
                }
                rows.add(toResult(record));
            }
            if (rows.isEmpty()) {
                throw new CaseLinkRowException("EMPTY_FILE", "The CSV must contain at least one data row");
            }
            return rows;
        } catch (IOException | IllegalArgumentException | java.io.UncheckedIOException exception) {
            throw new CaseLinkRowException("MALFORMED_CSV", "The file must contain well-formed UTF-8 CSV", exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (!"csv".equalsIgnoreCase(FilenameUtils.getExtension(file.getOriginalFilename()))
            || file.getContentType() == null || !CONTENT_TYPES.contains(file.getContentType())) {
            throw new CaseLinkRowException("INVALID_FILE_TYPE", "A CSV file is required");
        }
        if (file.isEmpty()) {
            throw new CaseLinkRowException("EMPTY_FILE", "The CSV file is empty");
        }
        if (file.getSize() > configuration.getMaxFileSize().toBytes()) {
            throw new CaseLinkRowException("FILE_TOO_LARGE", "The CSV exceeds the configured maximum file size");
        }
    }

    private CaseLinkImportResultEntity toResult(CSVRecord record) {
        var result = new CaseLinkImportResultEntity();
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
