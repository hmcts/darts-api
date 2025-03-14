package uk.gov.hmcts.darts.arm.component.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.CaseArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.AnnotationCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.arm.model.record.operation.CaseCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.arm.model.record.operation.MediaCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.arm.model.record.operation.TranscriptionCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.test.common.FileStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.UPLOAD_NEW_FILE;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@SuppressWarnings("PMD.AssignmentInOperand")
@ExtendWith(MockitoExtension.class)
@Slf4j
class ArchiveRecordFileGeneratorImplTest {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    private static final String READING_FILE = "Reading file {}";

    @TempDir
    private File tempDirectory;

    private ArchiveRecordFileGeneratorImpl archiveRecordFileGenerator;

    @BeforeEach
    void setUp() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();
        archiveRecordFileGenerator = new ArchiveRecordFileGeneratorImpl(objectMapper);
    }

    @AfterEach
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void clean() throws Exception {
        FileStore.getFileStore().remove();
        try (var filesStream = Files.list(tempDirectory.toPath())) {
            Assertions.assertEquals(0, filesStream.count());
        }
    }

    @Test
    void generateArchiveRecordWithMedia() throws IOException {
        String fileLocation = tempDirectory.getAbsolutePath();
        String relationId = "1234";
        File archiveFile = FileStore.getFileStore().create(Path.of(fileLocation), Path.of("1234-1-1.a360"));

        archiveRecordFileGenerator.generateArchiveRecord(createMediaArchiveRecord(relationId), archiveFile, ArchiveRecordType.MEDIA_ARCHIVE_TYPE);

        log.info(READING_FILE, archiveFile.getAbsolutePath());

        String actualResponse = getFileContents(archiveFile);
        String expectedResponse = getContentsFromFile("Tests/arm/component/ArchiveMediaMetadata/expectedResponse.a360");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void generateArchiveRecordWithTranscription() throws IOException {
        String fileLocation = tempDirectory.getAbsolutePath();
        String relationId = "1234";
        File archiveFile = FileStore.getFileStore().create(Path.of(fileLocation), Path.of("1234-1-1.a360"));

        archiveRecordFileGenerator.generateArchiveRecord(createTranscriptionArchiveRecord(relationId), archiveFile,
                                                         ArchiveRecordType.TRANSCRIPTION_ARCHIVE_TYPE);

        log.info(READING_FILE, archiveFile.getAbsolutePath());

        String actualResponse = getFileContents(archiveFile);
        String expectedResponse = getContentsFromFile("Tests/arm/component/ArchiveTranscriptionMetadata/expectedResponse.a360");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void generateArchiveRecordWithAnnotation() throws IOException {
        String fileLocation = tempDirectory.getAbsolutePath();
        String relationId = "1234";
        File archiveFile = FileStore.getFileStore().create(Path.of(fileLocation), Path.of("1234-1-1.a360"));

        archiveRecordFileGenerator.generateArchiveRecord(createAnnotationArchiveRecord(relationId), archiveFile, ArchiveRecordType.ANNOTATION_ARCHIVE_TYPE);

        log.info(READING_FILE, archiveFile.getAbsolutePath());

        String actualResponse = getFileContents(archiveFile);
        String expectedResponse = getContentsFromFile("Tests/arm/component/ArchiveAnnotationMetadata/expectedResponse.a360");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void generateArchiveRecordWithCase() throws IOException {
        String fileLocation = tempDirectory.getAbsolutePath();
        String relationId = "1234";
        File archiveFile = FileStore.getFileStore().create(Path.of(fileLocation), Path.of("1234-1-1.a360"));

        archiveRecordFileGenerator.generateArchiveRecord(createCaseArchiveRecord(relationId), archiveFile, ArchiveRecordType.CASE_ARCHIVE_TYPE);

        log.info(READING_FILE, archiveFile.getAbsolutePath());

        String actualResponse = getFileContents(archiveFile);
        String expectedResponse = getContentsFromFile("Tests/arm/component/ArchiveCaseMetadata/expectedResponse.a360");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void generateArchiveRecordWithNullArchiveRecord() throws Exception {
        String fileLocation = tempDirectory.getAbsolutePath();
        File archiveFile = FileStore.getFileStore().create(Path.of(fileLocation), Path.of("test-media-arm.a360"));

        boolean result = archiveRecordFileGenerator.generateArchiveRecord(null, archiveFile, ArchiveRecordType.MEDIA_ARCHIVE_TYPE);
        assertFalse(result);
    }

    @Test
    void generateArchiveRecords_typical() throws Exception {
        String relationId = "1234";

        var archiveRecord = createMediaArchiveRecord(relationId);
        List<ArchiveRecord> archiveRecords = List.of(archiveRecord, archiveRecord);

        String result = archiveRecordFileGenerator.generateArchiveRecords("archive-records.a360", archiveRecords);

        String expectedResponse = getContentsFromFile("Tests/arm/component/expectedResponseMultipleArchiveRecords.a360");
        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void generateArchiveRecords_emptyArchiveRecords() {
        List<ArchiveRecord> archiveRecords = Collections.emptyList();

        String result = archiveRecordFileGenerator.generateArchiveRecords("archive-records.a360", archiveRecords);
        assertThat(result).isBlank();
    }

    @Test
    void generateArchiveRecordsArchiveRecordError() throws Exception {
        var objectMapper = mock(ObjectMapper.class);
        archiveRecordFileGenerator = new ArchiveRecordFileGeneratorImpl(objectMapper);
        when(objectMapper.writeValueAsString(any())).thenThrow(RuntimeException.class);

        String relationId = "1234";
        var archiveRecord = createMediaArchiveRecord(relationId);
        List<ArchiveRecord> archiveRecords = List.of(archiveRecord);

        assertThrows(DartsException.class, () -> archiveRecordFileGenerator.generateArchiveRecords("archive-records.a360", archiveRecords));
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        return RecordMetadata.builder()
            .recordClass("DARTS")
            .publisher("DARTS")
            .region("GBR")
            .recordDate(recordTime.format(formatter))
            .eventDate("2024-01-23T11:40:00.000Z")
            .title("Filename")
            .clientId("1234")
            .contributor("Swansea & Courtroom 1")
            .bf001("Media")
            .bf002("Case_1|Case_2|Case_3")
            .bf003("mp2")
            .bf004("2024-01-23T00:00:00.000Z")
            .bf005("xi/XkzD2HuqTUzDafW8Cgw==")
            .bf011("2024-01-23T11:39:30.000Z")
            .bf012(1)
            .bf013(1)
            .bf014(3)
            .bf015(4)
            .bf017("2024-01-23T11:40:00.000Z")
            .bf018("2024-01-23T13:40:00.000Z")
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
        uploadNewFileRecordMetadata.setDzFilename("123_456_1");
        uploadNewFileRecordMetadata.setFileTag("mp2");
        return uploadNewFileRecordMetadata;
    }


    private TranscriptionArchiveRecord createTranscriptionArchiveRecord(String relationId) {
        return TranscriptionArchiveRecord.builder()
            .transcriptionCreateArchiveRecordOperation(createTranscriptionArchiveRecordOperation(relationId))
            .uploadNewFileRecord(createTranscriptionUploadNewFileRecord(relationId))
            .build();
    }

    private UploadNewFileRecord createTranscriptionUploadNewFileRecord(String relationId) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(relationId);
        uploadNewFileRecord.setFileMetadata(createTranscriptionUploadNewFileRecordMetadata());
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createTranscriptionUploadNewFileRecordMetadata() {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher("DARTS");
        uploadNewFileRecordMetadata.setDzFilename("123_456_1");
        uploadNewFileRecordMetadata.setFileTag("docx");
        return uploadNewFileRecordMetadata;
    }


    private TranscriptionCreateArchiveRecordOperation createTranscriptionArchiveRecordOperation(String relationId) {
        return TranscriptionCreateArchiveRecordOperation.builder()
            .relationId(relationId)
            .recordMetadata(createTranscriptionArchiveRecordMetadata())
            .build();
    }

    private RecordMetadata createTranscriptionArchiveRecordMetadata() {
        OffsetDateTime recordTime = OffsetDateTime.of(2024, 1, 23, 10, 0, 0, 0, ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        return RecordMetadata.builder()
            .recordClass("DARTS")
            .publisher("DARTS")
            .region("GBR")
            .recordDate(recordTime.format(formatter))
            .eventDate("2024-01-23T11:40:00.000Z")
            .title("Filename")
            .clientId("1234")
            .contributor("Swansea & Courtroom 1")
            .bf001("Transcription")
            .bf002("Case_1|Case_2|Case_3")
            .bf003("mp2")
            .bf004("2024-01-23T00:00:00.000Z")
            .bf005("xi/XkzD2HuqTUzDafW8Cgw==")
            .bf011("2024-01-23T11:39:30.000Z")
            .bf012(1)
            .bf013(1)
            .bf017("2024-01-23T11:40:00.000Z")
            .bf018("2024-01-23T13:40:00.000Z")
            .bf019("Swansea")
            .bf020("Courtroom 1")
            .build();
    }


    private AnnotationArchiveRecord createAnnotationArchiveRecord(String relationId) {
        return AnnotationArchiveRecord.builder()
            .annotationCreateArchiveRecordOperation(createAnnotationArchiveRecordOperation(relationId))
            .uploadNewFileRecord(createAnnotationUploadNewFileRecord(relationId))
            .build();
    }

    private UploadNewFileRecord createAnnotationUploadNewFileRecord(String relationId) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(relationId);
        uploadNewFileRecord.setFileMetadata(createAnnotationUploadNewFileRecordMetadata());
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createAnnotationUploadNewFileRecordMetadata() {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher("DARTS");
        uploadNewFileRecordMetadata.setDzFilename("123_456_1");
        uploadNewFileRecordMetadata.setFileTag("txt");
        return uploadNewFileRecordMetadata;
    }

    private AnnotationCreateArchiveRecordOperation createAnnotationArchiveRecordOperation(String relationId) {
        return AnnotationCreateArchiveRecordOperation.builder()
            .relationId(relationId)
            .recordMetadata(createAnnotationArchiveRecordMetadata())
            .build();
    }

    private RecordMetadata createAnnotationArchiveRecordMetadata() {
        OffsetDateTime recordTime = OffsetDateTime.of(2024, 1, 23, 10, 0, 0, 0, ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        return RecordMetadata.builder()
            .recordClass("DARTS")
            .publisher("DARTS")
            .region("GBR")
            .recordDate(recordTime.format(formatter))
            .eventDate("2024-01-23T11:40:00.000Z")
            .title("Filename")
            .clientId("1234")
            .contributor("Swansea & Courtroom 1")
            .bf001("Annotation")
            .bf002("Case_1|Case_2|Case_3")
            .bf003("txt")
            .bf004("2024-01-23T00:00:00.000Z")
            .bf005("xi/XkzD2HuqTUzDafW8Cgw==")
            .bf011("2024-01-23T11:39:30.000Z")
            .bf012(1)
            .bf013(1)
            .bf017("2024-01-23T11:40:00.000Z")
            .bf018("2024-01-23T13:40:00.000Z")
            .bf019("Swansea")
            .bf020("Courtroom 1")
            .build();
    }

    private CaseArchiveRecord createCaseArchiveRecord(String relationId) {
        return CaseArchiveRecord.builder()
            .caseCreateArchiveRecordOperation(createCaseArchiveRecordOperation(relationId))
            .uploadNewFileRecord(createCaseUploadNewFileRecord(relationId))
            .build();
    }

    private UploadNewFileRecord createCaseUploadNewFileRecord(String relationId) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(relationId);
        uploadNewFileRecord.setFileMetadata(createCaseUploadNewFileRecordMetadata());
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createCaseUploadNewFileRecordMetadata() {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher("DARTS");
        uploadNewFileRecordMetadata.setDzFilename("123_456_1");
        uploadNewFileRecordMetadata.setFileTag("txt");
        return uploadNewFileRecordMetadata;
    }

    private CaseCreateArchiveRecordOperation createCaseArchiveRecordOperation(String relationId) {
        return CaseCreateArchiveRecordOperation.builder()
            .relationId(relationId)
            .recordMetadata(createCaseArchiveRecordMetadata())
            .build();
    }

    private RecordMetadata createCaseArchiveRecordMetadata() {
        OffsetDateTime recordTime = OffsetDateTime.of(2024, 1, 23, 10, 0, 0, 0, ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        return RecordMetadata.builder()
            .recordClass("DARTS")
            .publisher("DARTS")
            .region("GBR")
            .recordDate(recordTime.format(formatter))
            .eventDate("2024-01-23T11:40:00.000Z")
            .title("Filename")
            .clientId("1234")
            .contributor("Swansea")
            .bf001("Case")
            .bf002("Case_1|Case_2|Case_3")
            .bf003("txt")
            .bf004("2024-01-23T00:00:00.000Z")
            .bf005("xi/XkzD2HuqTUzDafW8Cgw==")
            .bf011("2024-01-23T11:39:30.000Z")
            .bf012(1)
            .bf013(1)
            .bf017("2024-01-23T11:40:00.000Z")
            .bf018("2024-01-23T13:40:00.000Z")
            .bf019("Swansea")
            .build();
    }
}