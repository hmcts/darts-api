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
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.MediaCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.UPLOAD_NEW_FILE;
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
        File archiveFile = new File(fileLocation, "1234-1-1.a360");
        archiveRecordFileGenerator.generateArchiveRecord(createMediaArchiveRecord(relationId), archiveFile, ArchiveRecordType.MEDIA_ARCHIVE_TYPE);

        log.info("Reading file {}", archiveFile.getAbsolutePath());

        String actualResponse = getFileContents(archiveFile);
        String expectedResponse = getContentsFromFile("Tests/arm/component/ArchiveMediaMetadata/expectedResponse.a360");
        log.info("actual Response {}", actualResponse);
        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void generateArchiveRecordWithNullArchiveRecord() {
        String fileLocation = tempDirectory.getAbsolutePath();
        File archiveFile = new File(fileLocation, "test-media-arm.a360");
        ArchiveRecord archiveRecord = null;
        boolean result = archiveRecordFileGenerator.generateArchiveRecord(archiveRecord, archiveFile, ArchiveRecordType.MEDIA_ARCHIVE_TYPE);
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


    private RecordMetadata createMediaArchiveRecordMetadata() {
        OffsetDateTime recordTime = OffsetDateTime.of(2024, 1, 23, 10, 0, 0, 0, ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return RecordMetadata.builder()
            .recordClass("DARTSMedia")
            .publisher("DARTS")
            .region("GBR")
            .recordDate(recordTime.format(formatter))
            .eventDate("2024-01-23T11:40:00Z")
            .title("Filename")
            .clientId("1234")
            .contributor("Swansea & Courtroom 1")
            .bf001("Media")
            .bf002("Case_1|Case_2|Case_3")
            .bf003("mp2")
            .bf004("2024-01-23")
            .bf005("xi/XkzD2HuqTUzDafW8Cgw==")
            .bf011("2024-01-23T11:39:30Z")
            .bf012(1)
            .bf013(1)
            .bf014(3)
            .bf015(4)
            .bf017("2024-01-23T11:40:00Z")
            .bf018("2024-01-23T13:40:00Z")
            .bf019("Swansea")
            .bf020("Courtroom 1")
            .build();
    }

    private UploadNewFileRecord createMediaUploadNewFileRecord(String relationId) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(relationId);
        uploadNewFileRecord.setFileMetadata(createMediaUploadNewFileRecordMetadata());
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createMediaUploadNewFileRecordMetadata() {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher("DARTS");
        uploadNewFileRecordMetadata.setDzFilename("123_456_1.mp2");
        uploadNewFileRecordMetadata.setFileTag("mp2");
        return uploadNewFileRecordMetadata;

    }
}
