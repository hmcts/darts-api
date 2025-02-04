package uk.gov.hmcts.darts.arm.component.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.model.ArchiveRecordOperation;
import uk.gov.hmcts.darts.common.exception.DartsException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.isNull;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArchiveRecordFileGeneratorImpl implements ArchiveRecordFileGenerator {

    private final ObjectMapper objectMapper;

    @Override
    public boolean generateArchiveRecord(ArchiveRecord archiveRecord, File archiveRecordFile, ArchiveRecordType archiveRecordType) {
        if (isNull(archiveRecord) || isNull(archiveRecordFile)) {
            log.error("Unable to generate {} arm record due to invalid data", archiveRecordType);
            return false;
        }
        boolean generatedArchiveRecord = false;
        try {
            String archiveRecordOperation = objectMapper.writeValueAsString(archiveRecord.getArchiveRecordOperation());
            String uploadNewFileRecord = objectMapper.writeValueAsString(archiveRecord.getUploadNewFileRecord());
            log.debug("About to write {}{} to file {}", archiveRecordOperation, uploadNewFileRecord, archiveRecordFile.getAbsolutePath());
            try (BufferedWriter fileWriter = Files.newBufferedWriter(archiveRecordFile.toPath()); PrintWriter printWriter = new PrintWriter(fileWriter)) {
                printWriter.println(archiveRecordOperation);
                printWriter.println(uploadNewFileRecord);
                generatedArchiveRecord = true;
            }
        } catch (IOException e) {
            log.error("Unable to write ARM file {}", archiveRecordFile.getAbsoluteFile(), e);
        }
        return generatedArchiveRecord;
    }

    @Override
    public String generateArchiveRecords(String archiveFileName, List<ArchiveRecord> archiveRecords) {
        StringBuilder archiveRecordsStringBuilder = new StringBuilder();
        String archiveRecordsString = null;
        if (!archiveRecords.isEmpty()) {
            for (var archiveRecord : archiveRecords) {
                try {
                    String archiveRecordOperation = objectMapper.writeValueAsString(archiveRecord.getArchiveRecordOperation());
                    String uploadNewFileRecord = objectMapper.writeValueAsString(archiveRecord.getUploadNewFileRecord());
                    archiveRecordsStringBuilder.append(archiveRecordOperation).append(System.lineSeparator());
                    archiveRecordsStringBuilder.append(uploadNewFileRecord).append(System.lineSeparator());
                } catch (Exception e) {
                    log.error("Unable to write archive record for EOD {}",
                              archiveRecord.getArchiveRecordOperation().getRelationId());
                    throw new DartsException(e);
                }
            }
            archiveRecordsString = archiveRecordsStringBuilder.toString();
            List<String> relationIds = getRelationIds(archiveRecords);
            log.info("Contents of manifest file {} for EOD's {}\n{}",
                     archiveFileName,
                     StringUtils.join(relationIds, ", "),
                     archiveRecordsString);
        }
        return archiveRecordsString;
    }

    private static List<String> getRelationIds(List<ArchiveRecord> archiveRecords) {
        return archiveRecords.stream()
            .map(ArchiveRecord::getArchiveRecordOperation)
            .filter(Objects::nonNull)
            .map(ArchiveRecordOperation::getRelationId)
            .filter(Objects::nonNull)
            .toList();
    }
}
