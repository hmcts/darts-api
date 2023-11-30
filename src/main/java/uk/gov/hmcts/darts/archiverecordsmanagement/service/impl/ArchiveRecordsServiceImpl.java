package uk.gov.hmcts.darts.archiverecordsmanagement.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.darts.archiverecordsmanagement.exception.ArchiveRecordApiError.FAILED_TO_GENERATE_MEDIA_ARCHIVE_RECORD;

@AllArgsConstructor
@Slf4j
public class ArchiveRecordsServiceImpl {

    private ObjectMapper objectMapper;

    public void generateMediaArchiveRecord(MediaArchiveRecord mediaArchiveRecord, File mediaMetadataFile) {
        if (isNull(mediaArchiveRecord) || isNull(mediaMetadataFile)) {
            log.error("Unable to generate media archive record due to invalid data");
            throw new DartsApiException(FAILED_TO_GENERATE_MEDIA_ARCHIVE_RECORD);
        }
        try {
            String archiveRecord1 = objectMapper.writeValueAsString(mediaArchiveRecord.getCreateArchiveRecord());
            String archiveRecord2 = objectMapper.writeValueAsString(mediaArchiveRecord.getUploadNewFileRecord());
            log.info(archiveRecord1 + archiveRecord2);
            try (BufferedWriter fileWriter = Files.newBufferedWriter(mediaMetadataFile.toPath());
                 PrintWriter printWriter = new PrintWriter(fileWriter)) {

                printWriter.println(archiveRecord1);
                printWriter.println(archiveRecord2);
            } catch (IOException e) {
                log.error("Unable to write ARM media file {}, due to {}", mediaMetadataFile.getAbsoluteFile(), e.getMessage());
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
