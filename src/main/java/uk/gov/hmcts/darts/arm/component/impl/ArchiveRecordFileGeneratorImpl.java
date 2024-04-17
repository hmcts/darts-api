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

    public boolean generateArchiveRecord(ArchiveRecord archiveRecord, File archiveRecordFile, ArchiveRecordType archiveRecordType) {
        boolean generatedArchiveRecord = false;
        if (isNull(archiveRecord) || isNull(archiveRecordFile)) {
            log.error("Unable to generate {} arm record due to invalid data", archiveRecordType);
            return false;
        }
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

    /* TODO: This is a temporary method that is used to help debug issues with the manifest file and should be removed */
    @Deprecated
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
