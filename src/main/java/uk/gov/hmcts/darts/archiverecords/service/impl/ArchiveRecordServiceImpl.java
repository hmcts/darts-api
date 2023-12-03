package uk.gov.hmcts.darts.archiverecords.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.archiverecords.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.archiverecords.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.archiverecords.exception.ArchiveRecordApiError.FAILED_TO_GENERATE_ARCHIVE_RECORD;

@AllArgsConstructor
@Slf4j
public class ArchiveRecordsServiceImpl {


    public void generateArchiveRecord(ExternalObjectDirectoryEntity externalObjectDirectory, File archiveRecordFile) {
        if (nonNull(externalObjectDirectory.getMedia())) {
            MediaArchiveRecord mediaArchiveRecord = createMediaArchiveRecord(externalObjectDirectory);
            generateArchiveRecord(mediaArchiveRecord, archiveRecordFile, ArchiveRecordType.MEDIA_ARCHIVE_TYPE);
        }
        if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {

        }
        if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {

        }
    }




}
