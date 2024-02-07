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
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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
    public static final String File_EXTENTION = "a360";
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);


    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryEntity;
    @Mock
    private MediaEntity mediaEntity;
    @Mock
    private TranscriptionEntity transcriptionEntity;
    @Mock
    private TranscriptionDocumentEntity transcriptionDocumentEntity;
    @Mock
    private AnnotationDocumentEntity annotationDocumentEntity;
    @Mock
    private CourtroomEntity courtroomEntity;
    @Mock
    private CourthouseEntity courthouseEntity;
    @Mock
    private TranscriptionCommentEntity transcriptionCommentEntity;
    @Mock
    private TranscriptionUrgencyEntity transcriptionUrgencyEntity;
    @Mock
    private TranscriptionTypeEntity transcriptionTypeEntity;
    @Mock
    private UserAccountEntity userAccountEntity;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @TempDir
    private File tempDirectory;

    @InjectMocks
    private ArchiveRecordServiceImpl archiveRecordService;

    @BeforeEach
    void setUp() {
        ArchiveRecordFileGenerator archiveRecordFileGenerator = new ArchiveRecordFileGeneratorImpl(getObjectMapper());

        MediaArchiveRecordMapper mediaArchiveRecordMapper = new MediaArchiveRecordMapperImpl(armDataManagementConfiguration, currentTimeHelper);
        TranscriptionArchiveRecordMapper transcriptionArchiveRecordMapper = new TranscriptionArchiveRecordMapperImpl(
                armDataManagementConfiguration,
                currentTimeHelper
        );
        AnnotationArchiveRecordMapper annotationArchiveRecordMapper = new AnnotationArchiveRecordMapperImpl(armDataManagementConfiguration, currentTimeHelper);
        CaseArchiveRecordMapper caseArchiveRecordMapper = new CaseArchiveRecordMapperImpl(armDataManagementConfiguration, currentTimeHelper);

        archiveRecordService = new ArchiveRecordServiceImpl(
                armDataManagementConfiguration,
                archiveRecordFileGenerator,
                mediaArchiveRecordMapper,
                transcriptionArchiveRecordMapper,
                annotationArchiveRecordMapper,
                caseArchiveRecordMapper,
                externalObjectDirectoryRepository
        );

    }

    @Test
    void generateArchiveRecord_WithLiveMediaProperties_ReturnFileSuccess() throws IOException {

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

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("DARTSMedia");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(File_EXTENTION);
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn("Tests/arm/properties/live/media-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(ArchiveRecordServiceImplTest.DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID);

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
    void generateArchiveRecord_WithNleMediaProperties_ReturnFileSuccess() throws IOException {

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
        when(mediaEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        when(mediaEntity.getMediaFile()).thenReturn(TEST_MEDIA_ARCHIVE_A_360);
        when(mediaEntity.getMediaFormat()).thenReturn(MP_2);
        when(mediaEntity.getStart()).thenReturn(startedAt);
        when(mediaEntity.getEnd()).thenReturn(endedAt);
        when(mediaEntity.getCreatedDateTime()).thenReturn(startedAt);
        when(mediaEntity.getCaseNumberList()).thenReturn(List.of("Case1", "Case2", "Case3"));

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("DARTSMedia");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(File_EXTENTION);
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn("Tests/arm/properties/nle/media-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(ArchiveRecordServiceImplTest.DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID);

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
    void givenNoData_WhenGenerateArchiveRecord_ReturnEmptyList() {
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID);

        assertFalse(archiveRecordFileInfo.isFileGenerationSuccessful());
    }

    @Test
    void generateArchiveRecord_WithNleTranscriptionProperties_ReturnFileSuccess() throws IOException {

        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);
        when(courtroomEntity.getName()).thenReturn("Room1");

        when(userAccountEntity.getUserFullName()).thenReturn("Test User");

        when(transcriptionCommentEntity.getComment()).thenReturn("Test transcription comment");

        when(transcriptionEntity.getTranscriptionCommentEntities()).thenReturn(List.of(transcriptionCommentEntity));
        when(transcriptionEntity.getTranscriptionUrgency()).thenReturn(transcriptionUrgencyEntity);
        when(transcriptionEntity.getCourtroom()).thenReturn(courtroomEntity);

        when(transcriptionDocumentEntity.getId()).thenReturn(1);
        when(transcriptionDocumentEntity.getTranscription()).thenReturn(transcriptionEntity);
        when(transcriptionDocumentEntity.getUploadedBy()).thenReturn(userAccountEntity);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getTranscriptionRecordClass()).thenReturn("DARTSTranscription");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(File_EXTENTION);
        when(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile()).thenReturn("Tests/arm/properties/nle/transcription-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(ArchiveRecordServiceImplTest.DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateTranscriptionArchiveRecord/nle/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("eResponse {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void generateArchiveRecord_WithLiveTranscriptionProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);
        when(courtroomEntity.getName()).thenReturn("Room1");

        when(userAccountEntity.getUserFullName()).thenReturn("Test User");

        when(transcriptionCommentEntity.getComment()).thenReturn("Test transcription comment");

        when(transcriptionEntity.getTranscriptionCommentEntities()).thenReturn(List.of(transcriptionCommentEntity));
        when(transcriptionEntity.getTranscriptionUrgency()).thenReturn(transcriptionUrgencyEntity);
        when(transcriptionEntity.getCourtroom()).thenReturn(courtroomEntity);

        when(transcriptionDocumentEntity.getId()).thenReturn(1);
        when(transcriptionDocumentEntity.getTranscription()).thenReturn(transcriptionEntity);
        when(transcriptionDocumentEntity.getUploadedBy()).thenReturn(userAccountEntity);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getTranscriptionRecordClass()).thenReturn("DARTSTranscription");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(File_EXTENTION);
        when(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile()).thenReturn("Tests/arm/properties/live/transcription-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(ArchiveRecordServiceImplTest.DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateTranscriptionArchiveRecord/nle/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("eResponse {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void generateArchiveRecord_WithAllPropertiesTranscriptionProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);
        when(courtroomEntity.getName()).thenReturn("Room1");

        when(userAccountEntity.getUserFullName()).thenReturn("Test User");

        when(transcriptionCommentEntity.getComment()).thenReturn("Test transcription comment");
        when(transcriptionUrgencyEntity.getDescription()).thenReturn("STANDARD");
        when(transcriptionTypeEntity.getDescription()).thenReturn("SPECIFIED_TIMES");

        OffsetDateTime startedAt = testTime.minusHours(1);
        OffsetDateTime endedAt = testTime.plusHours(2);

        when(transcriptionEntity.getTranscriptionCommentEntities()).thenReturn(List.of(transcriptionCommentEntity));
        when(transcriptionEntity.getTranscriptionUrgency()).thenReturn(transcriptionUrgencyEntity);
        when(transcriptionEntity.getCourtroom()).thenReturn(courtroomEntity);
        when(transcriptionEntity.getStartTime()).thenReturn(startedAt);
        when(transcriptionEntity.getEndTime()).thenReturn(endedAt);
        when(transcriptionEntity.getIsManualTranscription()).thenReturn(true);
        when(transcriptionEntity.getHearingDate()).thenReturn(LocalDate.of(2023, 1, 1));
        when(transcriptionEntity.getTranscriptionType()).thenReturn(transcriptionTypeEntity);

        when(transcriptionDocumentEntity.getId()).thenReturn(1);
        when(transcriptionDocumentEntity.getTranscription()).thenReturn(transcriptionEntity);
        when(transcriptionDocumentEntity.getUploadedBy()).thenReturn(userAccountEntity);
        when(transcriptionDocumentEntity.getFileType()).thenReturn("docx");
        when(transcriptionDocumentEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        when(transcriptionDocumentEntity.getUploadedDateTime()).thenReturn(startedAt);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getTranscriptionRecordClass()).thenReturn("DARTSTranscription");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(File_EXTENTION);
        when(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile()).thenReturn(
                "Tests/arm/properties/all_properties/transcription-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(ArchiveRecordServiceImplTest.DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateTranscriptionArchiveRecord/all_properties/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("eResponse {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void generateArchiveRecord_WithNleAnnotationProperties_ReturnFileSuccess() throws IOException {

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        when(annotationDocumentEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getFileExtension()).thenReturn(File_EXTENTION);
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(ArchiveRecordServiceImplTest.DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        when(armDataManagementConfiguration.getAnnotationRecordPropertiesFile()).thenReturn(
                "Tests/arm/properties/nle/annotation-record.properties");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateAnnotationArchiveRecord/nle/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("eResponse {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithLiveAnnotationProperties_ReturnFileSuccess() throws IOException {
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        when(annotationDocumentEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getFileExtension()).thenReturn(File_EXTENTION);
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(ArchiveRecordServiceImplTest.DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        when(armDataManagementConfiguration.getAnnotationRecordPropertiesFile()).thenReturn(
                "Tests/arm/properties/live/annotation-record.properties");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateAnnotationArchiveRecord/nle/expectedResponse.a360");
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
