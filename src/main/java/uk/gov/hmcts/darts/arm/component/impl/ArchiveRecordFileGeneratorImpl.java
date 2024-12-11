package uk.gov.hmcts.darts.arm.component.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.common.exception.DartsException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
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
    public String generateArchiveRecords(String archvieFileName, List<ArchiveRecord> archiveRecords) {
        StringBuilder archiveRecordsStringBuilder = new StringBuilder();
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
        }
        String archiveRecordsString = archiveRecordsStringBuilder.toString();
        log.info("Contents of manifest file {} for EOD {}\n{}",
                 archvieFileName,
                 archiveRecords.get(0).getArchiveRecordOperation().getRelationId(),
                 archiveRecordsString);
        return archiveRecordsString;
    }


    @Override
    @Deprecated(since = "11/12/2024")
    public void generateArchiveRecords(List<ArchiveRecord> archiveRecords, File archiveRecordsFile) {
        if (!archiveRecords.isEmpty()) {
            try (BufferedWriter fileWriter = Files.newBufferedWriter(archiveRecordsFile.toPath()); PrintWriter printWriter = new PrintWriter(fileWriter)) {
                for (var archiveRecord : archiveRecords) {
                    try {
                        String archiveRecordOperation = objectMapper.writeValueAsString(archiveRecord.getArchiveRecordOperation());
                        String uploadNewFileRecord = objectMapper.writeValueAsString(archiveRecord.getUploadNewFileRecord());
                        log.debug("About to write {}{} to file {} for EOD {}", archiveRecordOperation, uploadNewFileRecord,
                                  archiveRecordsFile.getAbsolutePath(), archiveRecord.getArchiveRecordOperation().getRelationId());
                        printWriter.println(archiveRecordOperation);
                        printWriter.println(uploadNewFileRecord);
                    } catch (Exception e) {
                        log.error("Unable to write archive record for EOD {} to ARM file {}",
                                  archiveRecord.getArchiveRecordOperation().getRelationId(),
                                  archiveRecordsFile.getAbsoluteFile());
                        throw new DartsException(e);
                    }
                }
            } catch (IOException e) {
                log.error("Unable to write ARM manifest file {}", archiveRecordsFile.getAbsoluteFile(), e);
                throw new DartsException(e);
            }
            logManifestFile(archiveRecords, archiveRecordsFile);
        }
    }

    @Deprecated(since = "11/12/2024")
    private void logManifestFile(List<ArchiveRecord> archiveRecords, File archiveRecordsFile) {
        try {
            String contents = FileUtils.readFileToString(archiveRecordsFile.getAbsoluteFile(), UTF_8);
            log.info("Contents of manifest file {} for EOD {}\n{}",
                     archiveRecordsFile.getAbsoluteFile(),
                     archiveRecords.get(0).getArchiveRecordOperation().getRelationId(),
                     contents);
        } catch (Exception e) {
            log.error("Unable to read ARM manifest file {}", archiveRecordsFile.getAbsoluteFile(), e);
        }
    }
}
