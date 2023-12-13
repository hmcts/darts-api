package uk.gov.hmcts.darts.arm.component.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.darts.arm.exception.ArchiveRecordApiError.FAILED_TO_GENERATE_ARCHIVE_RECORD;

@Component
@AllArgsConstructor
@Slf4j
public class ArchiveRecordFileGeneratorImpl {

    private ObjectMapper objectMapper;

    public boolean generateArchiveRecord(ArchiveRecord archiveRecord, File archiveRecordFile, ArchiveRecordType archiveRecordType) {
        boolean generatedArchiveRecord = false;
        if (isNull(archiveRecord) || isNull(archiveRecordFile)) {
            log.error("Unable to generate {} arm record due to invalid data", archiveRecordType);
            throw new DartsApiException(FAILED_TO_GENERATE_ARCHIVE_RECORD);
        }
        try {
            String archiveRecordOperation = objectMapper.writeValueAsString(archiveRecord.getArchiveRecordOperation());
            String uploadNewFileRecord = objectMapper.writeValueAsString(archiveRecord.getUploadNewFileRecord());
            log.info("About to write {}{} to file {}", archiveRecordOperation, uploadNewFileRecord, archiveRecordFile.getAbsolutePath());
            try (BufferedWriter fileWriter = Files.newBufferedWriter(archiveRecordFile.toPath()); PrintWriter printWriter = new PrintWriter(fileWriter)) {
                // Print all on 1 line
                printWriter.print(archiveRecordOperation);
                printWriter.println(uploadNewFileRecord);
                generatedArchiveRecord = true;
            } catch (IOException e) {
                log.error("Unable to write ARM file {}, due to {}", archiveRecordFile.getAbsoluteFile(), e.getMessage());
            }
        } catch (JsonProcessingException e) {
            log.error("Unable to write arm record: {}", e.getMessage());
            throw new DartsApiException(FAILED_TO_GENERATE_ARCHIVE_RECORD);
        }
        return generatedArchiveRecord;
    }
}
