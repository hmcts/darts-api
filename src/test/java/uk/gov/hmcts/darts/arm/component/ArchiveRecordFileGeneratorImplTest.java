package uk.gov.hmcts.darts.arm.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.arm.component.impl.ArchiveRecordFileGeneratorImpl;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.MediaCreateArchiveRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.MediaCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@SuppressWarnings("PMD.AssignmentInOperand")
@ExtendWith(MockitoExtension.class)
@Slf4j
class ArchiveRecordFileGeneratorImplTest {

    @TempDir
    private File tempDirectory;

    private ArchiveRecordFileGeneratorImpl archiveRecordFileGenerator;

    @BeforeEach
    void setUp() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();
        archiveRecordFileGenerator = new ArchiveRecordFileGeneratorImpl(objectMapper);
    }

    @Test
    void generateArchiveRecordWithMedia() throws IOException {
        String fileLocation = tempDirectory.getAbsolutePath();
        String relationId = "1234";
        File archiveFile = new File(fileLocation, "test-media-arm.a360");
        archiveRecordFileGenerator.generateArchiveRecord(createMediaArchiveRecord(relationId), archiveFile, ArchiveRecordType.MEDIA_ARCHIVE_TYPE);

        log.info("Reading file " + archiveFile.getAbsolutePath());

        String actualResponse = getFileContents(archiveFile);
        String expectedResponse = getContentsFromFile("Tests/arm/component/ArchiveMediaMetadata/expectedResponse.a360");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void generateArchiveRecordWithNullArchiveRecord() {
        String fileLocation = tempDirectory.getAbsolutePath();
        File archiveFile = new File(fileLocation, "test-media-arm.a360");
        boolean result = archiveRecordFileGenerator.generateArchiveRecord(null, archiveFile, ArchiveRecordType.MEDIA_ARCHIVE_TYPE);
        assertFalse(result);
    }

    private static String getFileContents(File archiveFile) throws IOException {
        StringBuilder fileContents = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(archiveFile.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContents.append(line);
            }
        }
        return fileContents.toString();
    }

    private MediaArchiveRecord createMediaArchiveRecord(String relationId) {
        return MediaArchiveRecord.builder()
            .mediaCreateArchiveRecord(createMediaArchiveRecordOperation(relationId))
            .uploadNewFileRecord(createMediaUploadNewFileRecord(relationId))
            .build();
    }

    private MediaCreateArchiveRecordOperation createMediaArchiveRecordOperation(String relationId) {
        return MediaCreateArchiveRecordOperation.builder()
            .relationId(relationId)
            .recordMetadata(createMediaArchiveRecordMetadata())
            .build();
    }

    private MediaCreateArchiveRecordMetadata createMediaArchiveRecordMetadata() {
        return MediaCreateArchiveRecordMetadata.builder()
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


    private UploadNewFileRecord createMediaUploadNewFileRecord(String relationId) {
        return UploadNewFileRecord.builder()
            .relationId(relationId)
            .fileMetadata(createMediaUploadNewFileRecordMetadata())
            .build();

    }

    private UploadNewFileRecordMetadata createMediaUploadNewFileRecordMetadata() {
        return UploadNewFileRecordMetadata.builder()
            .publisher("DARTS")
            .dzFilename("123_456_1.mp2") // <EOD>_<MEDID>_<ATTEMPT>.mp2"
            .fileTag("mp2")
            .build();

    }
}
