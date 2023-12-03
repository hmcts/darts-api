package uk.gov.hmcts.darts.archiverecordsmanagement.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.metadata.CreateArchiveRecordMetadata;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.record.CreateArchiveRecord;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.common.util.TestUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchiveRecordsServiceImplTest {

    @Autowired
    private ObjectMapper objectMapper;

    @TempDir
    private File tempDirectory;

    private ArchiveRecordsServiceImpl archiveRecordsService;

    @BeforeEach
    void setUp() {
        objectMapper = TestUtils.getObjectMapper();
        archiveRecordsService = new ArchiveRecordsServiceImpl(objectMapper);
    }

    @Test
    void generateMediaArchiveRecord() throws IOException, URISyntaxException {
        String fileLocation = tempDirectory.getAbsolutePath();
        String relationId = "<EODID>";
        File archiveFile = new File(fileLocation, "test-media-archive.a360");
        archiveRecordsService.generateMediaArchiveRecord(createMediaArchiveRecord(relationId), archiveFile);

        log.debug("Reading file " + archiveFile.getAbsolutePath());

        String actualResponse = "";

        try (BufferedReader reader = Files.newBufferedReader(archiveFile.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                actualResponse+=line;
            }
        }
        String expectedResponse = getContentsFromFile("tests/archive/ArchiveMediaMetadata/expectedResponse.a360");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    private MediaArchiveRecord createMediaArchiveRecord(String relationId) {
        return MediaArchiveRecord.builder()
            .createArchiveRecord(createArchiveRecord(relationId))
            .uploadNewFileRecord(createUploadNewFileRecord(relationId))
            .build();
    }

    private CreateArchiveRecord createArchiveRecord(String relationId) {
        return CreateArchiveRecord.builder()
            .relationId(relationId)
            .recordMetadata(createArchiveRecordMetadata())
            .build();
    }

    private CreateArchiveRecordMetadata createArchiveRecordMetadata() {
        return CreateArchiveRecordMetadata.builder()
            .publisher("DARTS")
            .recordClass("DARTSMedia")
            .recordDate("2023-07-19T11:39:30Z")
            .region("GBR")
            .id("12345")
            .type("Media")
            .channel("1")
            .maxChannels("4")
            .courthouse("Swansea")
            .courtroom("1234")
            .mediaFile("media_filename")
            .mediaFormat("mp2")
            .startDateTime("2023-07-18T11:39:30Z")
            .endDateTime("2023-07-18T12:39:30Z")
            .createdDateTime("2023-07-14T12:39:30Z")
            .caseNumbers("Case_1|Case_2|Case_3")
            .build();
    }


    private UploadNewFileRecord createUploadNewFileRecord(String relationId) {
        UploadNewFileRecord uploadNewFileRecord = UploadNewFileRecord.builder()
            .relationId(relationId)
            .fileMetadata(createUploadNewFileRecordMetadata())
            .build();
        return uploadNewFileRecord;

    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata() {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = UploadNewFileRecordMetadata.builder()
            .publisher("DARTS")
            .dzFilename("<EOD>_<MEDID>_<ATTEMPT>.mp2")
            .fileTag("mp2")
            .build();
        return uploadNewFileRecordMetadata;

    }


}
