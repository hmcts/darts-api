package uk.gov.hmcts.darts.arm.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.component.impl.ArchiveRecordFileGeneratorImpl;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.enums.ArchiveRecordType;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.impl.AnnotationArchiveRecordMapperImpl;
import uk.gov.hmcts.darts.arm.mapper.impl.MediaArchiveRecordMapperImpl;
import uk.gov.hmcts.darts.arm.mapper.impl.TranscriptionArchiveRecordMapperImpl;
import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;
import uk.gov.hmcts.darts.arm.service.impl.ArchiveRecordServiceImpl;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.util.TestUtils.getObjectMapper;

@SuppressWarnings("PMD.AssignmentInOperand")
@ExtendWith(MockitoExtension.class)
@Slf4j
class ArchiveRecordServiceImplTest {

    public static final String TEST_MEDIA_ARCHIVE_A_360 = "test-media-arm.a360";
    public static final String TEST_TRANSCRIPTION_ARCHIVE_A_360 = "test-transcription-arm.a360";
    public static final String TEST_ANNOTATION_ARCHIVE_A_360 = "test-annotation-arm.a360";
    public static final String MP_2 = "mp2";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DARTS = "DARTS";
    public static final String REGION = "GBR";
    public static final int EODID = 1234;

    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryEntity;
    @Mock
    private MediaEntity mediaEntity;
    @Mock
    private TranscriptionDocumentEntity transcriptionDocumentEntity;
    @Mock
    private AnnotationDocumentEntity annotationDocumentEntity;
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
        when(externalObjectDirectoryRepository.getReferenceById(anyInt())).thenReturn(externalObjectDirectoryEntity);

        ArchiveRecordFileGenerator archiveRecordFileGenerator = new ArchiveRecordFileGeneratorImpl(getObjectMapper());

        MediaArchiveRecordMapper mediaArchiveRecordMapper = new MediaArchiveRecordMapperImpl(armDataManagementConfiguration);
        TranscriptionArchiveRecordMapper transcriptionArchiveRecordMapper = new TranscriptionArchiveRecordMapperImpl(armDataManagementConfiguration);
        AnnotationArchiveRecordMapper annotationArchiveRecordMapper = new AnnotationArchiveRecordMapperImpl(armDataManagementConfiguration);

        archiveRecordService = new ArchiveRecordServiceImpl(armDataManagementConfiguration,
                                                            externalObjectDirectoryRepository,
                                                            archiveRecordFileGenerator,
                                                            mediaArchiveRecordMapper,
                                                            transcriptionArchiveRecordMapper,
                                                            annotationArchiveRecordMapper
        );

    }

    @Test
    void givenGenerateArchiveRecord_WithMedia() throws IOException {
        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);
        when(courtroomEntity.getName()).thenReturn("Room1");

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        when(mediaEntity.getCourtroom()).thenReturn(courtroomEntity);
        when(mediaEntity.getChannel()).thenReturn(1);
        when(mediaEntity.getTotalChannels()).thenReturn(4);
        when(mediaEntity.getMediaFile()).thenReturn(TEST_MEDIA_ARCHIVE_A_360);
        when(mediaEntity.getMediaFormat()).thenReturn(MP_2);
        when(mediaEntity.getStart()).thenReturn(startedAt);
        when(mediaEntity.getEnd()).thenReturn(endedAt);
        when(mediaEntity.getCreatedDateTime()).thenReturn(startedAt);
        when(mediaEntity.getCaseIdList()).thenReturn(List.of("Case1", "Case2", "Case3"));

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getMedia()).thenReturn(mediaEntity);

        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("DARTSMedia");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(".a360");

        Map<String, ArchiveRecordFileInfo> archiveRecordFiles = archiveRecordService.generateArchiveRecord(1234, 1);
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordFiles.get(ArchiveRecordType.MEDIA_ARCHIVE_TYPE.getArchiveTypeDescription());
        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actualResponse {}", actualResponse);

        assertTrue(actualResponse.startsWith("{\"operation\":\"create_record\""));
    }

    @Test
    void generateArchiveRecordWithNoData() {
        Map<String, ArchiveRecordFileInfo> archiveRecordFiles = archiveRecordService.generateArchiveRecord(1234, 1);
        assertTrue(archiveRecordFiles.isEmpty());
    }

    @Test
    void generateArchiveRecordWithTranscription() {
        when(externalObjectDirectoryEntity.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);

        assertThrows(NotImplementedException.class, () ->
            archiveRecordService.generateArchiveRecord(1, 2));

    }

    @Test
    void generateArchiveRecordWithAnnotation() {
        when(externalObjectDirectoryEntity.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);

        assertThrows(NotImplementedException.class, () ->
            archiveRecordService.generateArchiveRecord(1, 3));
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


}
