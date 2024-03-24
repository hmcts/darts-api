package uk.gov.hmcts.darts.arm.component.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;

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
                printWriter.print(archiveRecordOperation);
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
                        log.debug("About to write {}{} to file {}", archiveRecordOperation, uploadNewFileRecord, archiveRecordsFile.getAbsolutePath());
                        printWriter.print(archiveRecordOperation);
                        printWriter.println(uploadNewFileRecord);
                    } catch (Exception e) {
                        log.error("Unable to write archive record for EOD {} to ARM file {}",
                                  archiveRecord.getArchiveRecordOperation().getRelationId(),
                                  archiveRecordsFile.getAbsoluteFile());
                        //TODO is this the correct exception type?
                        throw new RuntimeException(e);
                    }
                }
            } catch (IOException e) {
                log.error("Unable to write ARM file {}", archiveRecordsFile.getAbsoluteFile(), e);
                throw new RuntimeException(e);
            }
        }
    }
}
