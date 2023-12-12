package uk.gov.hmcts.darts.arm.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.impl.AnnotationArchiveRecordMapperImpl;
import uk.gov.hmcts.darts.arm.mapper.impl.MediaArchiveRecordMapperImpl;
import uk.gov.hmcts.darts.arm.mapper.impl.TranscriptionArchiveRecordMapperImpl;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.MediaCreateArchiveRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.MediaCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.arm.service.impl.ArchiveRecordServiceImpl;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@ExtendWith(MockitoExtension.class)
@Slf4j
class ArchiveRecordServiceImplTest {

    public static final String TEST_MEDIA_ARCHIVE_A_360 = "test-media-arm.a360";
    public static final String MP_2 = "mp2";

    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Mock
    private ArchiveRecordFileGenerator archiveRecordFileGenerator;

    private MediaArchiveRecordMapper mediaArchiveRecordMapper;
    private TranscriptionArchiveRecordMapper transcriptionArchiveRecordMapper;
    private AnnotationArchiveRecordMapper annotationArchiveRecordMapper;

    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryEntity;
    @Mock
    private MediaEntity mediaEntity;
    @Mock
    private CourtroomEntity courtroomEntity;
    @Mock
    private CourthouseEntity courthouseEntity;

    @TempDir
    private File tempDirectory;

    @InjectMocks
    private ArchiveRecordServiceImpl archiveRecordService;

    @BeforeEach
    void setUp() {
        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);
        when(courtroomEntity.getName()).thenReturn("Room1");

        when(mediaEntity.getCourtroom()).thenReturn(courtroomEntity);
        when(mediaEntity.getChannel()).thenReturn(1);
        when(mediaEntity.getTotalChannels()).thenReturn(4);
        when(mediaEntity.getMediaFile()).thenReturn(TEST_MEDIA_ARCHIVE_A_360);
        when(mediaEntity.getMediaFormat()).thenReturn(MP_2);
        when(mediaEntity.getStart()).thenReturn(startedAt);
        when(mediaEntity.getEnd()).thenReturn(endedAt);
        when(mediaEntity.getCreatedDateTime()).thenReturn(startedAt);
        when(mediaEntity.getCaseIdList()).thenReturn(List.of("Case1", "Case2"));

        when(externalObjectDirectoryEntity.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryRepository.getReferenceById(anyInt())).thenReturn(externalObjectDirectoryEntity);

        mediaArchiveRecordMapper = new MediaArchiveRecordMapperImpl();
        transcriptionArchiveRecordMapper = new TranscriptionArchiveRecordMapperImpl();
        annotationArchiveRecordMapper = new AnnotationArchiveRecordMapperImpl();

        archiveRecordService = new ArchiveRecordServiceImpl(armDataManagementConfiguration,
                                                            externalObjectDirectoryRepository,
                                                            archiveRecordFileGenerator,
                                                            mediaArchiveRecordMapper,
                                                            transcriptionArchiveRecordMapper,
                                                            annotationArchiveRecordMapper);

    }

    @Test
    void generateArchiveRecordWithMedia() throws IOException {
        String fileLocation = tempDirectory.getAbsolutePath();
        String relationId = "<EODID>";
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        File archiveRecordFile = archiveRecordService.generateArchiveRecord(1, relationId, TEST_MEDIA_ARCHIVE_A_360);

        log.info("Reading file " + archiveRecordFile);

        String actualResponse = getFileContents(archiveRecordFile.getAbsoluteFile());
        String expectedResponse = getContentsFromFile("Tests/arm/ArchiveMediaMetadata/expectedResponse.a360");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
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
            .mediaCreateArchiveRecord(createArchiveRecord(relationId))
            .uploadNewFileRecord(createUploadNewFileRecord(relationId))
            .build();
    }

    private MediaCreateArchiveRecordOperation createArchiveRecord(String relationId) {
        return MediaCreateArchiveRecordOperation.builder()
            .relationId(relationId)
            .recordMetadata(createArchiveRecordMetadata())
            .build();
    }

    private MediaCreateArchiveRecordMetadata createArchiveRecordMetadata() {
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
            .mediaFormat(MP_2)
            .startDateTime("2023-07-18T11:39:30Z")
            .endDateTime("2023-07-18T12:39:30Z")
            .createdDateTime("2023-07-14T12:39:30Z")
            .caseNumbers("Case_1|Case_2|Case_3")
            .build();
    }


    private UploadNewFileRecord createUploadNewFileRecord(String relationId) {
        return UploadNewFileRecord.builder()
            .relationId(relationId)
            .fileMetadata(createUploadNewFileRecordMetadata())
            .build();

    }

    private UploadNewFileRecordMetadata createUploadNewFileRecordMetadata() {
        return UploadNewFileRecordMetadata.builder()
            .publisher("DARTS")
            .dzFilename("<EOD>_<MEDID>_<ATTEMPT>.mp2")
            .fileTag(MP_2)
            .build();

    }
}
