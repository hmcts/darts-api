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
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

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

import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.common.util.TestUtils.getObjectMapper;

@SuppressWarnings("PMD.AssignmentInOperand")
@ExtendWith(MockitoExtension.class)
@Slf4j
class ArchiveRecordServiceImplTest {
    public static final String TEST_ARCHIVE_FILENAME = "1234-1-1.a360";
    public static final String MP_2 = "mp2";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
    public static final String DARTS = "DARTS";
    public static final String REGION = "GBR";
    public static final int EODID = 1234;
    public static final String FILE_EXTENSION = "a360";
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
    private CaseDocumentEntity caseDocumentEntity;
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
    private TranscriptionWorkflowEntity transcriptionWorkflowEntity;
    @Mock
    private TranscriptionStatusEntity transcriptionStatusEntity;
    @Mock
    private AnnotationEntity annotationEntity;
    @Mock
    private HearingEntity hearingEntity1;
    @Mock
    private HearingEntity hearingEntity2;
    @Mock
    private HearingEntity hearingEntity3;
    @Mock
    private CourtCaseEntity courtCaseEntity1;
    @Mock
    private CourtCaseEntity courtCaseEntity2;
    @Mock
    private CourtCaseEntity courtCaseEntity3;
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

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

    }

    @Test
    void generateArchiveRecord_WithLiveMediaProperties_ReturnFileSuccess() throws IOException {

        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);
        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);
        when(courtroomEntity.getName()).thenReturn("Room1");

        OffsetDateTime startedAt = testTime.minusHours(1);

        when(courtCaseEntity1.getCaseNumber()).thenReturn("Case1");
        when(courtCaseEntity2.getCaseNumber()).thenReturn("Case2");
        when(courtCaseEntity3.getCaseNumber()).thenReturn("Case3");
        when(hearingEntity1.getCourtCase()).thenReturn(courtCaseEntity1);
        when(hearingEntity2.getCourtCase()).thenReturn(courtCaseEntity2);
        when(hearingEntity3.getCourtCase()).thenReturn(courtCaseEntity3);
        when(hearingEntity1.getHearingDate()).thenReturn(LocalDate.of(2023, 1, 1));

        OffsetDateTime endedAt = testTime;

        when(mediaEntity.getId()).thenReturn(1);
        when(mediaEntity.getCourtroom()).thenReturn(courtroomEntity);
        when(mediaEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        when(mediaEntity.getChannel()).thenReturn(1);
        when(mediaEntity.getTotalChannels()).thenReturn(4);
        when(mediaEntity.getMediaFile()).thenReturn(TEST_ARCHIVE_FILENAME);
        when(mediaEntity.getMediaFormat()).thenReturn(MP_2);
        when(mediaEntity.getEnd()).thenReturn(endedAt);
        when(mediaEntity.getCreatedDateTime()).thenReturn(startedAt);
        when(mediaEntity.getStart()).thenReturn(startedAt);
        when(mediaEntity.getHearingList()).thenReturn(List.of(hearingEntity1, hearingEntity2, hearingEntity3));

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn("Tests/arm/properties/live/media-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(ArchiveRecordServiceImplTest.DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

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

        when(courtCaseEntity1.getCaseNumber()).thenReturn("Case1");
        when(courtCaseEntity2.getCaseNumber()).thenReturn("Case2");
        when(courtCaseEntity3.getCaseNumber()).thenReturn("Case3");
        when(hearingEntity1.getCourtCase()).thenReturn(courtCaseEntity1);
        when(hearingEntity2.getCourtCase()).thenReturn(courtCaseEntity2);
        when(hearingEntity3.getCourtCase()).thenReturn(courtCaseEntity3);
        when(hearingEntity1.getHearingDate()).thenReturn(LocalDate.of(2023, 1, 1));

        OffsetDateTime startedAt = testTime.minusHours(1);
        OffsetDateTime endedAt = testTime;

        when(mediaEntity.getId()).thenReturn(1);
        when(mediaEntity.getCourtroom()).thenReturn(courtroomEntity);
        when(mediaEntity.getChannel()).thenReturn(1);
        when(mediaEntity.getTotalChannels()).thenReturn(4);
        when(mediaEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        when(mediaEntity.getMediaFile()).thenReturn(TEST_ARCHIVE_FILENAME);
        when(mediaEntity.getMediaFormat()).thenReturn(MP_2);
        when(mediaEntity.getStart()).thenReturn(startedAt);
        when(mediaEntity.getEnd()).thenReturn(endedAt);
        when(mediaEntity.getCreatedDateTime()).thenReturn(startedAt);
        when(mediaEntity.getHearingList()).thenReturn(List.of(hearingEntity1, hearingEntity2, hearingEntity3));

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn("Tests/arm/properties/nle/media-record.properties");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

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
    void generateArchiveRecord_WithAllMediaProperties_ReturnFileSuccess() throws IOException {

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
        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("DARTS");

        when(courtCaseEntity1.getCaseNumber()).thenReturn("Case1");
        when(courtCaseEntity2.getCaseNumber()).thenReturn("Case2");
        when(courtCaseEntity3.getCaseNumber()).thenReturn("Case3");
        when(hearingEntity1.getCourtCase()).thenReturn(courtCaseEntity1);
        when(hearingEntity2.getCourtCase()).thenReturn(courtCaseEntity2);
        when(hearingEntity3.getCourtCase()).thenReturn(courtCaseEntity3);
        when(hearingEntity1.getHearingDate()).thenReturn(LocalDate.of(2023, 1, 1));

        OffsetDateTime startedAt = testTime.minusHours(1);
        OffsetDateTime endedAt = testTime;

        when(mediaEntity.getId()).thenReturn(1);
        when(mediaEntity.getCourtroom()).thenReturn(courtroomEntity);
        when(mediaEntity.getChannel()).thenReturn(1);
        when(mediaEntity.getTotalChannels()).thenReturn(4);
        when(mediaEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        when(mediaEntity.getMediaFile()).thenReturn(TEST_ARCHIVE_FILENAME);
        when(mediaEntity.getMediaFormat()).thenReturn(MP_2);
        when(mediaEntity.getEnd()).thenReturn(endedAt);
        when(mediaEntity.getCreatedDateTime()).thenReturn(startedAt);
        when(mediaEntity.getStart()).thenReturn(startedAt);
        when(mediaEntity.getHearingList()).thenReturn(List.of(hearingEntity1, hearingEntity2, hearingEntity3));

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/all_properties/media-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(ArchiveRecordServiceImplTest.DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual Response {}", actualResponse);

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateMediaArchiveRecord/all_properties/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithInvalidMediaPropertiesPath_ReturnFileSuccess() throws IOException {

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
        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("DARTS");

        OffsetDateTime startedAt = testTime.minusHours(1);

        when(mediaEntity.getId()).thenReturn(1);
        when(mediaEntity.getCourtroom()).thenReturn(courtroomEntity);
        when(mediaEntity.getMediaFile()).thenReturn(TEST_ARCHIVE_FILENAME);
        when(mediaEntity.getMediaFormat()).thenReturn(MP_2);
        when(mediaEntity.getCreatedDateTime()).thenReturn(startedAt);
        when(mediaEntity.getHearingList()).thenReturn(List.of(hearingEntity1, hearingEntity2, hearingEntity3));

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/invalid_properties_path/media-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(ArchiveRecordServiceImplTest.DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual Response {}", actualResponse);
        OffsetDateTime endedAt = testTime;

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateMediaArchiveRecord/invalid_properties_path/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithNleTranscriptionProperties_ReturnFileSuccess() throws IOException {

        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(armDataManagementConfiguration.getTranscriptionRecordClass()).thenReturn("DARTS");

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getName()).thenReturn("Room1");
        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);

        when(courtCaseEntity1.getCaseNumber()).thenReturn("Case1");
        when(courtCaseEntity2.getCaseNumber()).thenReturn("Case2");
        when(courtCaseEntity3.getCaseNumber()).thenReturn("Case3");

        when(userAccountEntity.getId()).thenReturn(0);

        when(transcriptionStatusEntity.getId()).thenReturn(TranscriptionStatusEnum.REQUESTED.getId());
        when(transcriptionWorkflowEntity.getTranscriptionStatus()).thenReturn(transcriptionStatusEntity);
        when(transcriptionCommentEntity.getTranscriptionWorkflow()).thenReturn(transcriptionWorkflowEntity);
        when(transcriptionCommentEntity.getComment()).thenReturn("Test transcription comment");

        when(transcriptionUrgencyEntity.getDescription()).thenReturn("STANDARD");
        when(transcriptionTypeEntity.getDescription()).thenReturn("SPECIFIED_TIMES");

        when(transcriptionEntity.getId()).thenReturn(1);
        when(transcriptionEntity.getTranscriptionCommentEntities()).thenReturn(List.of(transcriptionCommentEntity));
        when(transcriptionEntity.getTranscriptionUrgency()).thenReturn(transcriptionUrgencyEntity);
        when(transcriptionEntity.getCourtroom()).thenReturn(courtroomEntity);
        when(transcriptionEntity.getIsManualTranscription()).thenReturn(false);
        when(transcriptionEntity.getHearingDate()).thenReturn(LocalDate.of(2023, 1, 1));
        when(transcriptionEntity.getTranscriptionType()).thenReturn(transcriptionTypeEntity);
        when(transcriptionEntity.getCourtCases()).thenReturn(List.of(courtCaseEntity1, courtCaseEntity2, courtCaseEntity3));

        when(transcriptionDocumentEntity.getId()).thenReturn(1);
        when(transcriptionDocumentEntity.getTranscription()).thenReturn(transcriptionEntity);
        when(transcriptionDocumentEntity.getUploadedBy()).thenReturn(userAccountEntity);
        when(transcriptionDocumentEntity.getFileType()).thenReturn("docx");
        when(transcriptionDocumentEntity.getFileName()).thenReturn("transcription.docx");
        when(transcriptionDocumentEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        OffsetDateTime startedAt = testTime.minusHours(1);
        when(transcriptionDocumentEntity.getUploadedDateTime()).thenReturn(startedAt);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getTranscriptionRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile()).thenReturn("Tests/arm/properties/nle/transcription-record.properties");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime endedAt = testTime.plusHours(2);
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

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getName()).thenReturn("Room1");
        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);

        when(courtCaseEntity1.getCaseNumber()).thenReturn("Case1");
        when(courtCaseEntity2.getCaseNumber()).thenReturn("Case2");
        when(courtCaseEntity3.getCaseNumber()).thenReturn("Case3");

        when(userAccountEntity.getId()).thenReturn(0);

        when(transcriptionStatusEntity.getId()).thenReturn(TranscriptionStatusEnum.REQUESTED.getId());
        when(transcriptionWorkflowEntity.getTranscriptionStatus()).thenReturn(transcriptionStatusEntity);
        when(transcriptionCommentEntity.getTranscriptionWorkflow()).thenReturn(transcriptionWorkflowEntity);

        when(transcriptionCommentEntity.getComment()).thenReturn("Test transcription comment");
        when(transcriptionUrgencyEntity.getDescription()).thenReturn("STANDARD");
        when(transcriptionTypeEntity.getDescription()).thenReturn("SPECIFIED_TIMES");

        when(transcriptionEntity.getId()).thenReturn(1);
        when(transcriptionEntity.getTranscriptionCommentEntities()).thenReturn(List.of(transcriptionCommentEntity));
        when(transcriptionEntity.getTranscriptionUrgency()).thenReturn(transcriptionUrgencyEntity);
        when(transcriptionEntity.getCourtroom()).thenReturn(courtroomEntity);
        when(transcriptionEntity.getIsManualTranscription()).thenReturn(true);
        when(transcriptionEntity.getHearingDate()).thenReturn(LocalDate.of(2023, 1, 1));
        when(transcriptionEntity.getTranscriptionType()).thenReturn(transcriptionTypeEntity);
        when(transcriptionEntity.getCourtCases()).thenReturn(List.of(courtCaseEntity1, courtCaseEntity2, courtCaseEntity3));

        when(transcriptionDocumentEntity.getId()).thenReturn(1);
        when(transcriptionDocumentEntity.getTranscription()).thenReturn(transcriptionEntity);
        when(transcriptionDocumentEntity.getUploadedBy()).thenReturn(userAccountEntity);
        when(transcriptionDocumentEntity.getFileType()).thenReturn("docx");
        when(transcriptionDocumentEntity.getFileName()).thenReturn("transcription.docx");
        when(transcriptionDocumentEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        OffsetDateTime startedAt = testTime.minusHours(1);
        when(transcriptionDocumentEntity.getUploadedDateTime()).thenReturn(startedAt);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getTranscriptionRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile()).thenReturn("Tests/arm/properties/live/transcription-record.properties");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime endedAt = testTime.plusHours(2);
        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateTranscriptionArchiveRecord/live/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("eResponse {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void generateArchiveRecord_WithAllPropertiesTranscription_ReturnFileSuccess() throws IOException {
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getName()).thenReturn("Room1");
        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);

        when(courtCaseEntity1.getCaseNumber()).thenReturn("Case1");
        when(courtCaseEntity2.getCaseNumber()).thenReturn("Case2");
        when(courtCaseEntity3.getCaseNumber()).thenReturn("Case3");

        when(userAccountEntity.getId()).thenReturn(0);

        when(transcriptionStatusEntity.getId()).thenReturn(TranscriptionStatusEnum.REQUESTED.getId());
        when(transcriptionWorkflowEntity.getTranscriptionStatus()).thenReturn(transcriptionStatusEntity);
        when(transcriptionCommentEntity.getTranscriptionWorkflow()).thenReturn(transcriptionWorkflowEntity);
        when(transcriptionCommentEntity.getComment()).thenReturn("Test transcription comment");

        when(transcriptionUrgencyEntity.getDescription()).thenReturn("STANDARD");
        when(transcriptionTypeEntity.getDescription()).thenReturn("SPECIFIED_TIMES");

        OffsetDateTime startedAt = testTime.minusHours(1);
        OffsetDateTime endedAt = testTime.plusHours(2);

        when(transcriptionEntity.getId()).thenReturn(1);
        when(transcriptionEntity.getTranscriptionCommentEntities()).thenReturn(List.of(transcriptionCommentEntity));
        when(transcriptionEntity.getTranscriptionUrgency()).thenReturn(transcriptionUrgencyEntity);
        when(transcriptionEntity.getCourtroom()).thenReturn(courtroomEntity);
        when(transcriptionEntity.getStartTime()).thenReturn(startedAt);
        when(transcriptionEntity.getEndTime()).thenReturn(endedAt);
        when(transcriptionEntity.getIsManualTranscription()).thenReturn(true);
        when(transcriptionEntity.getHearingDate()).thenReturn(LocalDate.of(2023, 1, 1));
        when(transcriptionEntity.getTranscriptionType()).thenReturn(transcriptionTypeEntity);
        when(transcriptionEntity.getCourtCases()).thenReturn(List.of(courtCaseEntity1, courtCaseEntity2, courtCaseEntity3));

        when(transcriptionDocumentEntity.getId()).thenReturn(1);
        when(transcriptionDocumentEntity.getTranscription()).thenReturn(transcriptionEntity);
        when(transcriptionDocumentEntity.getUploadedBy()).thenReturn(userAccountEntity);
        when(transcriptionDocumentEntity.getFileType()).thenReturn("docx");
        when(transcriptionDocumentEntity.getFileName()).thenReturn("transcription.docx");
        when(transcriptionDocumentEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        when(transcriptionDocumentEntity.getUploadedDateTime()).thenReturn(startedAt);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getTranscriptionRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/all_properties/transcription-record.properties");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

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
    void generateArchiveRecord_WithInvalidTranscriptionPropertiesPath_ReturnFileSuccess() throws IOException {
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getName()).thenReturn("Room1");
        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);

        OffsetDateTime startedAt = testTime.minusHours(1);

        when(transcriptionEntity.getCourtroom()).thenReturn(courtroomEntity);

        when(transcriptionDocumentEntity.getId()).thenReturn(1);
        when(transcriptionDocumentEntity.getTranscription()).thenReturn(transcriptionEntity);
        when(transcriptionDocumentEntity.getFileName()).thenReturn("transcription.docx");
        when(transcriptionDocumentEntity.getUploadedDateTime()).thenReturn(startedAt);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getTranscriptionRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/invalidpath/transcription-record.properties");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime endedAt = testTime.plusHours(2);

        String expectedResponse = getContentsFromFile(
            "Tests/arm/service/testGenerateTranscriptionArchiveRecord/invalid_properties_path/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("eResponse {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void generateArchiveRecord_WithNleAnnotationProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getName()).thenReturn("Room1");

        when(courtCaseEntity1.getCaseNumber()).thenReturn("Case1");
        when(courtCaseEntity1.getCourthouse()).thenReturn(courthouseEntity);
        when(courtCaseEntity2.getCaseNumber()).thenReturn("Case2");
        when(courtCaseEntity3.getCaseNumber()).thenReturn("Case3");
        when(hearingEntity1.getCourtCase()).thenReturn(courtCaseEntity1);
        when(hearingEntity2.getCourtCase()).thenReturn(courtCaseEntity2);
        when(hearingEntity3.getCourtCase()).thenReturn(courtCaseEntity3);
        when(hearingEntity1.getHearingDate()).thenReturn(LocalDate.of(2023, 1, 1));

        when(hearingEntity1.getCourtroom()).thenReturn(courtroomEntity);

        when(annotationEntity.getId()).thenReturn(1);
        when(annotationEntity.getHearingList()).thenReturn(List.of(hearingEntity1, hearingEntity2, hearingEntity3));
        when(annotationEntity.getText()).thenReturn("Annotation comments");

        OffsetDateTime uploadedDateTime = testTime.minusHours(1);

        when(annotationDocumentEntity.getId()).thenReturn(1);
        when(annotationDocumentEntity.getAnnotation()).thenReturn(annotationEntity);
        when(annotationDocumentEntity.getUploadedBy()).thenReturn(userAccountEntity);
        when(annotationDocumentEntity.getFileType()).thenReturn("application/msword");
        when(annotationDocumentEntity.getFileName()).thenReturn("annotation.docx");
        when(annotationDocumentEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        when(annotationDocumentEntity.getUploadedDateTime()).thenReturn(uploadedDateTime);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getAnnotationRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        when(armDataManagementConfiguration.getAnnotationRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/nle/annotation-record.properties");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

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
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getName()).thenReturn("Room1");

        when(courtCaseEntity1.getCaseNumber()).thenReturn("Case1");
        when(courtCaseEntity1.getCourthouse()).thenReturn(courthouseEntity);
        when(courtCaseEntity2.getCaseNumber()).thenReturn("Case2");
        when(courtCaseEntity3.getCaseNumber()).thenReturn("Case3");
        when(hearingEntity1.getCourtCase()).thenReturn(courtCaseEntity1);
        when(hearingEntity2.getCourtCase()).thenReturn(courtCaseEntity2);
        when(hearingEntity3.getCourtCase()).thenReturn(courtCaseEntity3);
        when(hearingEntity1.getHearingDate()).thenReturn(LocalDate.of(2023, 1, 1));

        when(hearingEntity1.getCourtroom()).thenReturn(courtroomEntity);

        when(annotationEntity.getId()).thenReturn(1);
        when(annotationEntity.getHearingList()).thenReturn(List.of(hearingEntity1, hearingEntity2, hearingEntity3));
        when(annotationEntity.getText()).thenReturn("Annotation comments");

        OffsetDateTime uploadedDateTime = testTime.minusHours(1);

        when(annotationDocumentEntity.getId()).thenReturn(1);
        when(annotationDocumentEntity.getAnnotation()).thenReturn(annotationEntity);
        when(annotationDocumentEntity.getUploadedBy()).thenReturn(userAccountEntity);
        when(annotationDocumentEntity.getFileType()).thenReturn("application/msword");
        when(annotationDocumentEntity.getFileName()).thenReturn("annotation.docx");
        when(annotationDocumentEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        when(annotationDocumentEntity.getUploadedDateTime()).thenReturn(uploadedDateTime);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getAnnotationRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        when(armDataManagementConfiguration.getAnnotationRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/live/annotation-record.properties");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateAnnotationArchiveRecord/live/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("eResponse {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithAllAnnotationProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getName()).thenReturn("Room1");

        when(courtCaseEntity1.getCaseNumber()).thenReturn("Case1");
        when(courtCaseEntity1.getCourthouse()).thenReturn(courthouseEntity);
        when(courtCaseEntity2.getCaseNumber()).thenReturn("Case2");
        when(courtCaseEntity3.getCaseNumber()).thenReturn("Case3");
        when(hearingEntity1.getCourtCase()).thenReturn(courtCaseEntity1);
        when(hearingEntity2.getCourtCase()).thenReturn(courtCaseEntity2);
        when(hearingEntity3.getCourtCase()).thenReturn(courtCaseEntity3);
        when(hearingEntity1.getHearingDate()).thenReturn(LocalDate.of(2023, 1, 1));

        when(hearingEntity1.getCourtroom()).thenReturn(courtroomEntity);

        when(annotationEntity.getId()).thenReturn(1);
        when(annotationEntity.getHearingList()).thenReturn(List.of(hearingEntity1, hearingEntity2, hearingEntity3));
        when(annotationEntity.getText()).thenReturn("Annotation comments");

        OffsetDateTime uploadedDateTime = testTime.minusHours(1);

        when(annotationDocumentEntity.getId()).thenReturn(1);
        when(annotationDocumentEntity.getAnnotation()).thenReturn(annotationEntity);
        when(annotationDocumentEntity.getUploadedBy()).thenReturn(userAccountEntity);
        when(annotationDocumentEntity.getFileType()).thenReturn("application/msword");
        when(annotationDocumentEntity.getFileName()).thenReturn("annotation.docx");
        when(annotationDocumentEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        when(annotationDocumentEntity.getUploadedDateTime()).thenReturn(uploadedDateTime);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getAnnotationRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        when(armDataManagementConfiguration.getAnnotationRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/all_properties/annotation-record.properties");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateAnnotationArchiveRecord/all_properties/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("eResponse {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithInvalidAnnotationPropertiesPath_ReturnFileSuccess() throws IOException {
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtroomEntity.getName()).thenReturn("Room1");

        when(courtCaseEntity1.getCourthouse()).thenReturn(courthouseEntity);
        when(hearingEntity1.getCourtCase()).thenReturn(courtCaseEntity1);

        when(hearingEntity1.getCourtroom()).thenReturn(courtroomEntity);

        when(annotationEntity.getHearingList()).thenReturn(List.of(hearingEntity1, hearingEntity2, hearingEntity3));

        OffsetDateTime uploadedDateTime = testTime.minusHours(1);

        when(annotationDocumentEntity.getId()).thenReturn(1);
        when(annotationDocumentEntity.getAnnotation()).thenReturn(annotationEntity);
        when(annotationDocumentEntity.getFileName()).thenReturn("annotation.docx");
        when(annotationDocumentEntity.getUploadedDateTime()).thenReturn(uploadedDateTime);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getAnnotationRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        when(armDataManagementConfiguration.getAnnotationRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/invalidpath/annotation-record.properties");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateAnnotationArchiveRecord/invalid_properties_path/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("eResponse {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithNleCaseProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtCaseEntity1.getCaseNumber()).thenReturn("Case1");
        when(courtCaseEntity1.getCourthouse()).thenReturn(courthouseEntity);

        when(hearingEntity1.getHearingDate()).thenReturn(LocalDate.of(2023, 1, 1));

        when(courtCaseEntity1.getId()).thenReturn(1);
        when(courtCaseEntity1.getHearings()).thenReturn(List.of(hearingEntity1, hearingEntity2, hearingEntity3));

        OffsetDateTime uploadedDateTime = testTime.minusHours(1);

        when(caseDocumentEntity.getId()).thenReturn(1);
        when(caseDocumentEntity.getCourtCase()).thenReturn(courtCaseEntity1);
        when(caseDocumentEntity.getFileType()).thenReturn("application/msword");
        when(caseDocumentEntity.getFileName()).thenReturn("annotation.docx");
        when(caseDocumentEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        when(caseDocumentEntity.getUploadedTs()).thenReturn(uploadedDateTime);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getCaseDocument()).thenReturn(caseDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getCaseRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        when(armDataManagementConfiguration.getCaseRecordPropertiesFile()).thenReturn("Tests/arm/properties/nle/case-record.properties");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateCaseArchiveRecord/nle/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("eResponse {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithLiveCaseProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtCaseEntity1.getId()).thenReturn(1);
        when(courtCaseEntity1.getCaseNumber()).thenReturn("Case1");
        when(courtCaseEntity1.getCourthouse()).thenReturn(courthouseEntity);

        when(hearingEntity1.getHearingDate()).thenReturn(LocalDate.of(2023, 1, 1));

        when(courtCaseEntity1.getId()).thenReturn(1);
        when(courtCaseEntity1.getHearings()).thenReturn(List.of(hearingEntity1, hearingEntity2, hearingEntity3));

        OffsetDateTime uploadedDateTime = testTime.minusHours(1);

        when(caseDocumentEntity.getId()).thenReturn(1);
        when(caseDocumentEntity.getCourtCase()).thenReturn(courtCaseEntity1);
        when(caseDocumentEntity.getFileType()).thenReturn("application/msword");
        when(caseDocumentEntity.getFileName()).thenReturn("annotation.docx");
        when(caseDocumentEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        when(caseDocumentEntity.getUploadedTs()).thenReturn(uploadedDateTime);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getCaseDocument()).thenReturn(caseDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getCaseRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        when(armDataManagementConfiguration.getCaseRecordPropertiesFile()).thenReturn("Tests/arm/properties/live/case-record.properties");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual Response {}", actualResponse);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateCaseArchiveRecord/live/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithAllCaseProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtCaseEntity1.getCaseNumber()).thenReturn("Case1");
        when(courtCaseEntity1.getCourthouse()).thenReturn(courthouseEntity);

        when(hearingEntity1.getHearingDate()).thenReturn(LocalDate.of(2023, 1, 1));

        when(courtCaseEntity1.getId()).thenReturn(1);
        when(courtCaseEntity1.getHearings()).thenReturn(List.of(hearingEntity1, hearingEntity2, hearingEntity3));

        OffsetDateTime uploadedDateTime = testTime.minusHours(1);

        when(caseDocumentEntity.getId()).thenReturn(1);
        when(caseDocumentEntity.getCourtCase()).thenReturn(courtCaseEntity1);
        when(caseDocumentEntity.getFileType()).thenReturn("application/msword");
        when(caseDocumentEntity.getFileName()).thenReturn("annotation.docx");
        when(caseDocumentEntity.getChecksum()).thenReturn("xi/XkzD2HuqTUzDafW8Cgw==");
        when(caseDocumentEntity.getUploadedTs()).thenReturn(uploadedDateTime);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getCaseDocument()).thenReturn(caseDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getCaseRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        when(armDataManagementConfiguration.getCaseRecordPropertiesFile()).thenReturn("Tests/arm/properties/all_properties/case-record.properties");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateCaseArchiveRecord/all_properties/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", startedAt.format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", endedAt.format(formatter));
        log.info("eResponse {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithinInvalidCasePropertiesPath_ReturnFileSuccess() throws IOException {
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        when(courthouseEntity.getCourthouseName()).thenReturn("Swansea");

        when(courtCaseEntity1.getCourthouse()).thenReturn(courthouseEntity);

        OffsetDateTime uploadedDateTime = testTime.minusHours(1);

        when(caseDocumentEntity.getId()).thenReturn(1);
        when(caseDocumentEntity.getCourtCase()).thenReturn(courtCaseEntity1);
        when(caseDocumentEntity.getFileName()).thenReturn("annotation.docx");
        when(caseDocumentEntity.getUploadedTs()).thenReturn(uploadedDateTime);

        when(externalObjectDirectoryEntity.getId()).thenReturn(EODID);
        when(externalObjectDirectoryEntity.getCaseDocument()).thenReturn(caseDocumentEntity);
        when(externalObjectDirectoryEntity.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(EODID)).thenReturn(Optional.of(externalObjectDirectoryEntity));

        when(armDataManagementConfiguration.getCaseRecordClass()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        when(armDataManagementConfiguration.getCaseRecordPropertiesFile()).thenReturn("Tests/arm/properties/invalidpath/case-record.properties");

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(EODID, "1234_1_1");

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals("1234_1_1.a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("aResponse {}", actualResponse);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        String expectedResponse = getContentsFromFile("Tests/arm/service/testGenerateCaseArchiveRecord/invalid_properties_path/expectedResponse.a360");
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
