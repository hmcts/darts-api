package uk.gov.hmcts.darts.arm.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.component.impl.ArchiveRecordFileGeneratorImpl;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.CaseArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.impl.AnnotationArchiveRecordMapperImpl;
import uk.gov.hmcts.darts.arm.mapper.impl.CaseArchiveRecordMapperImpl;
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
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.common.util.TestUtils.getObjectMapper;

@SuppressWarnings("PMD.AssignmentInOperand")
@ExtendWith(MockitoExtension.class)
@Slf4j
class ArchiveRecordServiceImplTest {
    public static final String TEST_MEDIA_ARCHIVE_A_360 = "1234-1-1.a360";
    public static final String MP_2 = "mp2";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DARTS = "DARTS";
    public static final String REGION = "GBR";
    public static final int EODID = 1234;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);


    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
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

    @Autowired
    private ResourceLoader resourceLoader;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @TempDir
    private File tempDirectory;

    @InjectMocks
    private ArchiveRecordServiceImpl archiveRecordService;

    @BeforeEach
    void setUp() {
        ArchiveRecordFileGenerator archiveRecordFileGenerator = new ArchiveRecordFileGeneratorImpl(getObjectMapper());

        MediaArchiveRecordMapper mediaArchiveRecordMapper = new MediaArchiveRecordMapperImpl(armDataManagementConfiguration, currentTimeHelper);
        TranscriptionArchiveRecordMapper transcriptionArchiveRecordMapper = new TranscriptionArchiveRecordMapperImpl(armDataManagementConfiguration);
        AnnotationArchiveRecordMapper annotationArchiveRecordMapper = new AnnotationArchiveRecordMapperImpl(armDataManagementConfiguration);
        CaseArchiveRecordMapper caseArchiveRecordMapper = new CaseArchiveRecordMapperImpl(armDataManagementConfiguration);

        archiveRecordService = new ArchiveRecordServiceImpl(
            armDataManagementConfiguration,
            archiveRecordFileGenerator,
            mediaArchiveRecordMapper,
            transcriptionArchiveRecordMapper,
            annotationArchiveRecordMapper,
            caseArchiveRecordMapper
        );

    }

    @Test
    void givenMedia_WhenGenerateArchiveRecord_ReturnFileSuccess() throws IOException {

        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);
        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);
        when(courtroomEntity.getName()).thenReturn("Room1");

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);

        OffsetDateTime startedAt = testTime.minusHours(1);
        OffsetDateTime endedAt = testTime;

        when(mediaEntity.getId()).thenReturn(1);
        when(mediaEntity.getCourtroom()).thenReturn(courtroomEntity);
        when(mediaEntity.getChannel()).thenReturn(1);
        when(mediaEntity.getTotalChannels()).thenReturn(4);
        when(mediaEntity.getMediaFile()).thenReturn(TEST_MEDIA_ARCHIVE_A_360);
        when(mediaEntity.getMediaFormat()).thenReturn(MP_2);
        when(mediaEntity.getEnd()).thenReturn(endedAt);
        when(mediaEntity.getCreatedDateTime()).thenReturn(startedAt);
        when(mediaEntity.getCaseNumberList()).thenReturn(List.of("Case1", "Case2", "Case3"));

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("DARTSMedia");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn("Tests/arm/properties/live/media-record.properties");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn("yyyy-MM-dd");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(externalObjectDirectoryEntity);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual Response {}", actualResponse);

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateMediaArchiveRecord/live/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_ReturnFileSuccess() throws IOException {

        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);
        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);
        when(courtroomEntity.getName()).thenReturn("Room1");

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);

        OffsetDateTime startedAt = testTime.minusHours(1);
        OffsetDateTime endedAt = testTime;

        when(mediaEntity.getId()).thenReturn(1);
        when(mediaEntity.getCourtroom()).thenReturn(courtroomEntity);
        when(mediaEntity.getChannel()).thenReturn(1);
        when(mediaEntity.getTotalChannels()).thenReturn(4);
        when(mediaEntity.getMediaFile()).thenReturn(TEST_MEDIA_ARCHIVE_A_360);
        when(mediaEntity.getMediaFormat()).thenReturn(MP_2);
        when(mediaEntity.getStart()).thenReturn(startedAt);
        when(mediaEntity.getEnd()).thenReturn(endedAt);
        when(mediaEntity.getCreatedDateTime()).thenReturn(startedAt);
        when(mediaEntity.getCaseNumberList()).thenReturn(List.of("Case1", "Case2", "Case3"));

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("DARTSMedia");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn("Tests/arm/properties/nle/media-record.properties");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn("yyyy-MM-dd");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(externalObjectDirectoryEntity);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual Response {}", actualResponse);

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateMediaArchiveRecord/nle/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void givenNoData_WhenGenerateArchiveRecord_ReturnEmptyList() throws IOException {
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(externalObjectDirectoryEntity);

        assertFalse(archiveRecordFileInfo.isFileGenerationSuccessful());
    }

    @Test
    void givenTranscription_WhenGenerateArchiveRecord_ReturnTranscriptionFile() throws IOException {
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        when(transcriptionDocumentEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(externalObjectDirectoryEntity);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateTranscriptionArchiveRecord/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("eResponse {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void givenAnnotation_WhenGenerateArchiveRecord_ReturnAnnotationFile() throws IOException {
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        when(annotationDocumentEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(externalObjectDirectoryEntity);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateAnnotationArchiveRecord/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("eResponse {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
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
