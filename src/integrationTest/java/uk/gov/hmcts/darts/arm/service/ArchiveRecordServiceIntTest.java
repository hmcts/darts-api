package uk.gov.hmcts.darts.arm.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaTestData;

@Disabled("Impacted by V1_367__adding_not_null_constraints_part_4.sql")
@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports", "VariableDeclarationUsageDistance", "PMD.AssignmentInOperand"})
class ArchiveRecordServiceIntTest extends IntegrationBase {
    private static final OffsetDateTime YESTERDAY = OffsetDateTime.now(UTC).minusDays(1).withHour(9).withMinute(0)
        .withSecond(0).withNano(0);

    private static final String REQUESTED_TRANSCRIPTION_COMMENT = "Requested transcription";

    private static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 9, 23, 10, 0, 0);

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final String DARTS = "DARTS";
    private static final String REGION = "GBR";

    private static final String FILE_EXTENSION = "a360";
    public static final int RETENTION_CONFIDENCE_SCORE = 2;
    public static final String RETENTION_CONFIDENCE_REASON = "RetentionConfidenceReason";

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT).withZone(ZoneId.of("UTC"));

    @TempDir
    private File tempDirectory;

    @Autowired
    private AuthorisationStub authorisationStub;

    @MockBean
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @MockBean
    private CurrentTimeHelper currentTimeHelper;

    private HearingEntity hearing;


    @Autowired
    private ArchiveRecordService archiveRecordService;


    @BeforeEach
    void setUp() {

        hearing = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "Case1",
            "NEWCASTLE",
            "Int Test Courtroom 2",
            HEARING_DATE
        );

    }

    @Test
    void generateArchiveRecord_WithLiveMediaProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);
        OffsetDateTime endedAt = OffsetDateTime.of(2023, 9, 23, 13, 45, 0, 0, UTC);

        MediaEntity media = getMediaTestData().createMediaWith(
            hearing.getCourtroom(),
            startedAt,
            endedAt,
            1
        );
        MediaEntity savedMedia = dartsDatabase.getMediaRepository().saveAndFlush(media);
        savedMedia.setMediaFile("a-media-file.mp2");
        savedMedia.setCreatedDateTime(startedAt);
        savedMedia.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        savedMedia.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );
        hearing.addMedia(savedMedia);
        dartsDatabase.getHearingRepository().saveAndFlush(hearing);

        armEod.setTransferAttempts(1);
        dartsDatabase.getExternalObjectDirectoryRepository().saveAndFlush(armEod);

        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn("tests/arm/properties/media-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armEod.getId(), prefix);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals(prefix + ".a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual response {}", actualResponse);

        String expectedResponse = getContentsFromFile("tests/arm/service/testGenerateMediaArchiveRecord/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<PREFIX>", prefix);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(armEod.getId()));
        expectedResponse = expectedResponse.replaceAll("<OBJECT_ID>", String.valueOf(savedMedia.getId()));
        expectedResponse = expectedResponse.replaceAll("<PARENT_ID>", String.valueOf(savedMedia.getId()));
        expectedResponse = expectedResponse.replaceAll("<START_DATE_TIME>", media.getStart().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE_TIME>", media.getEnd().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<EVENT_DATE_TIME>", media.getCreatedDateTime().format(formatter));

        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithLiveMediaPropertiesAndRetentionDate_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        OffsetDateTime retainUntil = OffsetDateTime.of(2024, 9, 23, 13, 45, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);
        OffsetDateTime endedAt = OffsetDateTime.of(2023, 9, 23, 13, 45, 0, 0, UTC);

        MediaEntity media = getMediaTestData().createMediaWith(
            hearing.getCourtroom(),
            startedAt,
            endedAt,
            1
        );
        media.setRetainUntilTs(retainUntil);
        MediaEntity savedMedia = dartsDatabase.getMediaRepository().saveAndFlush(media);
        savedMedia.setMediaFile("a-media-file.mp2");
        savedMedia.setCreatedDateTime(startedAt);
        savedMedia.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        savedMedia.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );
        hearing.addMedia(savedMedia);
        dartsDatabase.getHearingRepository().saveAndFlush(hearing);

        armEod.setTransferAttempts(1);
        dartsDatabase.getExternalObjectDirectoryRepository().saveAndFlush(armEod);

        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn("tests/arm/properties/media-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armEod.getId(), prefix);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals(prefix + ".a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual response {}", actualResponse);

        String expectedResponse = getContentsFromFile("tests/arm/service/testGenerateMediaArchiveRecord/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<PREFIX>", prefix);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(armEod.getId()));
        expectedResponse = expectedResponse.replaceAll("<OBJECT_ID>", String.valueOf(savedMedia.getId()));
        expectedResponse = expectedResponse.replaceAll("<PARENT_ID>", String.valueOf(savedMedia.getId()));
        expectedResponse = expectedResponse.replaceAll("<START_DATE_TIME>", media.getStart().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE_TIME>", media.getEnd().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<EVENT_DATE_TIME>", media.getRetainUntilTs().format(formatter));

        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithAllMediaProperties_ReturnFileSuccess() throws IOException {

        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 14, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);
        OffsetDateTime endedAt = OffsetDateTime.of(2023, 9, 23, 14, 45, 0, 0, UTC);

        MediaEntity media = getMediaTestData().createMediaWith(
            hearing.getCourtroom(),
            startedAt,
            endedAt,
            1
        );
        MediaEntity savedMedia = dartsDatabase.getMediaRepository().saveAndFlush(media);
        savedMedia.setMediaFile("a-media-file.mp2");
        savedMedia.setCreatedDateTime(startedAt);
        savedMedia.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        savedMedia.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );
        hearing.addMedia(savedMedia);
        dartsDatabase.getHearingRepository().saveAndFlush(hearing);

        armEod.setTransferAttempts(1);
        dartsDatabase.getExternalObjectDirectoryRepository().saveAndFlush(armEod);

        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn("tests/arm/properties/all_properties/media-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        String prefix = String.format("%d_%d_1", armEod.getId(), savedMedia.getId());
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armEod.getId(), prefix);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals(prefix + ".a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual response {}", actualResponse);

        String expectedResponse = getContentsFromFile("tests/arm/service/testGenerateMediaArchiveRecord/all_properties/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<PREFIX>", prefix);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(armEod.getId()));
        expectedResponse = expectedResponse.replaceAll("<OBJECT_ID>", String.valueOf(savedMedia.getId()));
        expectedResponse = expectedResponse.replaceAll("<PARENT_ID>", String.valueOf(savedMedia.getId()));
        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    @Disabled("Impacted by V1_364_*.sql")
    void generateArchiveRecord_WithLiveTranscriptionProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);

        authorisationStub.givenTestSchema();
        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var courtCase = authorisationStub.getCourtCaseEntity();
        courtCase.setCaseNumber("Case2");
        dartsDatabase.getCaseRepository().save(courtCase);
        var transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveAwaitingAuthorisationTranscription(
            testUser,
            courtCase,
            hearing,
            REQUESTED_TRANSCRIPTION_COMMENT,
            startedAt);

        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        TranscriptionDocumentEntity transcriptionDocumentEntity = TranscriptionStub.createTranscriptionDocumentEntity(
            transcriptionEntity, fileName, fileType, fileSize, testUser, checksum, startedAt);
        transcriptionDocumentEntity.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        transcriptionDocumentEntity.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        dartsDatabase.getTranscriptionDocumentRepository().save(transcriptionDocumentEntity);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            transcriptionDocumentEntity,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getTranscriptionRecordClass()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile()).thenReturn(
            "tests/arm/properties/transcription-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        String prefix = String.format("%d_%d_1", armEod.getId(), transcriptionDocumentEntity.getId());
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armEod.getId(), prefix);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals(prefix + ".a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual response {}", actualResponse);

        String expectedResponse = getContentsFromFile("tests/arm/service/testGenerateTranscriptionArchiveRecord/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<PREFIX>", prefix);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(armEod.getId()));
        expectedResponse = expectedResponse.replaceAll("<OBJECT_ID>", String.valueOf(transcriptionDocumentEntity.getId()));
        expectedResponse = expectedResponse.replaceAll("<PARENT_ID>", String.valueOf(transcriptionEntity.getId()));
        expectedResponse = expectedResponse.replaceAll("<CASE_NUMBERS>", String.format("T%s", YESTERDAY.format(BASIC_ISO_DATE)) + "|Case1");
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_BY>", String.valueOf(testUser.getId()));
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_DATE_TIME>", transcriptionDocumentEntity.getUploadedDateTime().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<EVENT_DATE_TIME>", transcriptionDocumentEntity.getUploadedDateTime().format(formatter));
        log.info("expect response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    @Disabled("Impacted by V1_364_*.sql")
    void generateArchiveRecord_WithLiveTranscriptionPropertiesRetentionDate_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);
        OffsetDateTime retainUntil = OffsetDateTime.of(2024, 9, 23, 13, 0, 0, 0, UTC);

        authorisationStub.givenTestSchema();
        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var courtCase = authorisationStub.getCourtCaseEntity();
        courtCase.setCaseNumber("Case2");
        dartsDatabase.getCaseRepository().save(courtCase);
        var transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveAwaitingAuthorisationTranscription(
            testUser,
            courtCase,
            hearing,
            REQUESTED_TRANSCRIPTION_COMMENT,
            startedAt);

        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        TranscriptionDocumentEntity transcriptionDocumentEntity = TranscriptionStub.createTranscriptionDocumentEntity(
            transcriptionEntity, fileName, fileType, fileSize, testUser, checksum, startedAt);
        transcriptionDocumentEntity.setRetainUntilTs(retainUntil);
        transcriptionDocumentEntity.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        transcriptionDocumentEntity.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        dartsDatabase.getTranscriptionDocumentRepository().save(transcriptionDocumentEntity);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            transcriptionDocumentEntity,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getTranscriptionRecordClass()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile()).thenReturn(
            "tests/arm/properties/transcription-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        String prefix = String.format("%d_%d_1", armEod.getId(), transcriptionDocumentEntity.getId());
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armEod.getId(), prefix);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals(prefix + ".a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual response {}", actualResponse);

        String expectedResponse = getContentsFromFile("tests/arm/service/testGenerateTranscriptionArchiveRecord/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<PREFIX>", prefix);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(armEod.getId()));
        expectedResponse = expectedResponse.replaceAll("<OBJECT_ID>", String.valueOf(transcriptionDocumentEntity.getId()));
        expectedResponse = expectedResponse.replaceAll("<PARENT_ID>", String.valueOf(transcriptionEntity.getId()));
        expectedResponse = expectedResponse.replaceAll("<CASE_NUMBERS>", String.format("T%s", YESTERDAY.format(BASIC_ISO_DATE)) + "|Case1");
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_BY>", String.valueOf(testUser.getId()));
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_DATE_TIME>", transcriptionDocumentEntity.getUploadedDateTime().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<EVENT_DATE_TIME>", transcriptionDocumentEntity.getRetainUntilTs().format(formatter));
        log.info("expect response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    @Disabled("Impacted by V1_364_*.sql")
    void generateArchiveRecord_WithAllPropertiesTranscription_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);

        authorisationStub.givenTestSchema();
        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var courtCase = authorisationStub.getCourtCaseEntity();
        courtCase.setCaseNumber("Case2");
        dartsDatabase.getCaseRepository().save(courtCase);
        var transcriptionEntity = dartsDatabase.getTranscriptionStub().createAndSaveAwaitingAuthorisationTranscription(
            testUser,
            courtCase,
            hearing,
            REQUESTED_TRANSCRIPTION_COMMENT,
            startedAt);

        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        TranscriptionDocumentEntity transcriptionDocumentEntity = TranscriptionStub.createTranscriptionDocumentEntity(
            transcriptionEntity, fileName, fileType, fileSize, testUser, checksum, startedAt);
        transcriptionDocumentEntity.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        transcriptionDocumentEntity.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        dartsDatabase.getTranscriptionDocumentRepository().save(transcriptionDocumentEntity);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            transcriptionDocumentEntity,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getTranscriptionRecordClass()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile()).thenReturn(
            "tests/arm/properties/all_properties/transcription-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        String prefix = String.format("%d_%d_1", armEod.getId(), transcriptionDocumentEntity.getId());
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armEod.getId(), prefix);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals(prefix + ".a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual response {}", actualResponse);

        String expectedResponse = getContentsFromFile("tests/arm/service/testGenerateTranscriptionArchiveRecord/all_properties/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<PREFIX>", prefix);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(armEod.getId()));
        expectedResponse = expectedResponse.replaceAll("<OBJECT_ID>", String.valueOf(transcriptionDocumentEntity.getId()));
        expectedResponse = expectedResponse.replaceAll("<PARENT_ID>", String.valueOf(transcriptionEntity.getId()));
        expectedResponse = expectedResponse.replaceAll("<CASE_NUMBERS>", String.format("T%s", YESTERDAY.format(BASIC_ISO_DATE)));
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_BY>", String.valueOf(testUser.getId()));
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_DATE_TIME>", transcriptionDocumentEntity.getUploadedDateTime().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<START_DATE_TIME>", transcriptionDocumentEntity.getTranscription().getStartTime().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE_TIME>", transcriptionDocumentEntity.getTranscription().getEndTime().format(formatter));

        log.info("expect response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    @Disabled("Impacted by V1_364_*.sql")
    void generateArchiveRecord_WithLiveAnnotationProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);
        authorisationStub.givenTestSchema();

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        String testAnnotation = "TestAnnotation";
        AnnotationEntity annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(testUser, testAnnotation, hearing);
        dartsDatabase.getAnnotationRepository().save(annotation);

        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = OffsetDateTime.now();
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        AnnotationDocumentEntity annotationDocument = dartsDatabase.getAnnotationStub()
            .createAnnotationDocumentEntity(annotation, fileName, fileType, fileSize, testUser, uploadedDateTime, checksum);
        annotationDocument.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        annotationDocument.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        dartsDatabase.save(annotationDocument);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            annotationDocument,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getAnnotationRecordClass()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getAnnotationRecordPropertiesFile()).thenReturn(
            "tests/arm/properties/annotation-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        String prefix = String.format("%d_%d_1", armEod.getId(), annotationDocument.getId());
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armEod.getId(), prefix);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals(prefix + ".a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual response {}", actualResponse);

        String expectedResponse = getContentsFromFile("tests/arm/service/testGenerateAnnotationArchiveRecord/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<PREFIX>", prefix);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(armEod.getId()));
        expectedResponse = expectedResponse.replaceAll("<OBJECT_ID>", String.valueOf(annotationDocument.getId()));
        expectedResponse = expectedResponse.replaceAll("<PARENT_ID>", String.valueOf(annotationDocument.getAnnotation().getId()));
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_DATE_TIME>", annotationDocument.getUploadedDateTime().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<EVENT_DATE_TIME>", annotationDocument.getUploadedDateTime().format(formatter));

        log.info("expect response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    @Disabled("Impacted by V1_364_*.sql")
    void generateArchiveRecord_WithLiveAnnotationPropertiesRetentionDate_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        OffsetDateTime retainUntil = OffsetDateTime.of(2024, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);
        authorisationStub.givenTestSchema();

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        String testAnnotation = "TestAnnotation";
        AnnotationEntity annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(testUser, testAnnotation, hearing);
        dartsDatabase.getAnnotationRepository().save(annotation);

        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = OffsetDateTime.now();
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        AnnotationDocumentEntity annotationDocument = dartsDatabase.getAnnotationStub()
            .createAnnotationDocumentEntity(annotation, fileName, fileType, fileSize, testUser, uploadedDateTime, checksum);
        annotationDocument.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        annotationDocument.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        annotationDocument.setRetainUntilTs(retainUntil);
        dartsDatabase.save(annotationDocument);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            annotationDocument,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getAnnotationRecordClass()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getAnnotationRecordPropertiesFile()).thenReturn(
            "tests/arm/properties/annotation-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        String prefix = String.format("%d_%d_1", armEod.getId(), annotationDocument.getId());
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armEod.getId(), prefix);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals(prefix + ".a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual response {}", actualResponse);

        String expectedResponse = getContentsFromFile("tests/arm/service/testGenerateAnnotationArchiveRecord/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<PREFIX>", prefix);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(armEod.getId()));
        expectedResponse = expectedResponse.replaceAll("<OBJECT_ID>", String.valueOf(annotationDocument.getId()));
        expectedResponse = expectedResponse.replaceAll("<PARENT_ID>", String.valueOf(annotationDocument.getAnnotation().getId()));
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_DATE_TIME>", annotationDocument.getUploadedDateTime().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<EVENT_DATE_TIME>", annotationDocument.getRetainUntilTs().format(formatter));

        log.info("expect response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    @Disabled("Impacted by V1_364_*.sql")
    void generateArchiveRecord_WithAllAnnotationProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);
        authorisationStub.givenTestSchema();

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        String testAnnotation = "TestAnnotation";
        AnnotationEntity annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(testUser, testAnnotation, hearing);
        dartsDatabase.getAnnotationRepository().save(annotation);


        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = OffsetDateTime.now();
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        AnnotationDocumentEntity annotationDocument = dartsDatabase.getAnnotationStub()
            .createAnnotationDocumentEntity(annotation, fileName, fileType, fileSize, testUser, uploadedDateTime, checksum);
        annotationDocument.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        annotationDocument.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        dartsDatabase.save(annotationDocument);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            annotationDocument,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getAnnotationRecordClass()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getAnnotationRecordPropertiesFile()).thenReturn(
            "tests/arm/properties/all_properties/annotation-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        String prefix = String.format("%d_%d_1", armEod.getId(), annotationDocument.getId());
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armEod.getId(), prefix);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals(prefix + ".a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual response {}", actualResponse);

        String expectedResponse = getContentsFromFile("tests/arm/service/testGenerateAnnotationArchiveRecord/all_properties/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<PREFIX>", prefix);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(armEod.getId()));
        expectedResponse = expectedResponse.replaceAll("<OBJECT_ID>", String.valueOf(annotationDocument.getId()));
        expectedResponse = expectedResponse.replaceAll("<PARENT_ID>", String.valueOf(annotationDocument.getAnnotation().getId()));
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_DATE_TIME>", annotationDocument.getUploadedDateTime().format(formatter));

        log.info("expect response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithLiveCaseProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);

        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntity, uploadedBy);
        caseDocument.setFileName("test_case_document.docx");
        caseDocument.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        caseDocument.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        dartsDatabase.save(caseDocument);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            caseDocument,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getCaseRecordClass()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getCaseRecordPropertiesFile()).thenReturn(
            "tests/arm/properties/case-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        String prefix = String.format("%d_%d_1", armEod.getId(), caseDocument.getId());
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armEod.getId(), prefix);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals(prefix + ".a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual response {}", actualResponse);

        String expectedResponse = getContentsFromFile("tests/arm/service/testGenerateCaseArchiveRecord/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<PREFIX>", prefix);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(armEod.getId()));
        expectedResponse = expectedResponse.replaceAll("<OBJECT_ID>", String.valueOf(caseDocument.getId()));
        expectedResponse = expectedResponse.replaceAll("<PARENT_ID>", String.valueOf(courtCaseEntity.getId()));
        expectedResponse = expectedResponse.replaceAll("<CASE_NUMBERS>", String.format("T%s", YESTERDAY.format(BASIC_ISO_DATE)) + "|Case1");
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_BY>", String.valueOf(uploadedBy.getId()));
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_DATE_TIME>", caseDocument.getCreatedDateTime().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<EVENT_DATE_TIME>", caseDocument.getCreatedDateTime().format(formatter));

        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithLiveCasePropertiesRetentionDate_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        OffsetDateTime retainUntil = OffsetDateTime.of(2024, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);

        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntity, uploadedBy);
        caseDocument.setFileName("test_case_document.docx");
        caseDocument.setRetainUntilTs(retainUntil);
        caseDocument.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        caseDocument.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        dartsDatabase.save(caseDocument);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            caseDocument,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getCaseRecordClass()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getCaseRecordPropertiesFile()).thenReturn(
            "tests/arm/properties/case-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        String prefix = String.format("%d_%d_1", armEod.getId(), caseDocument.getId());
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armEod.getId(), prefix);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals(prefix + ".a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual response {}", actualResponse);

        String expectedResponse = getContentsFromFile("tests/arm/service/testGenerateCaseArchiveRecord/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<PREFIX>", prefix);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(armEod.getId()));
        expectedResponse = expectedResponse.replaceAll("<OBJECT_ID>", String.valueOf(caseDocument.getId()));
        expectedResponse = expectedResponse.replaceAll("<PARENT_ID>", String.valueOf(courtCaseEntity.getId()));
        expectedResponse = expectedResponse.replaceAll("<CASE_NUMBERS>", String.format("T%s", YESTERDAY.format(BASIC_ISO_DATE)) + "|Case1");
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_BY>", String.valueOf(uploadedBy.getId()));
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_DATE_TIME>", caseDocument.getCreatedDateTime().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<EVENT_DATE_TIME>", caseDocument.getRetainUntilTs().format(formatter));

        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithAllCaseProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);

        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntity, uploadedBy);
        caseDocument.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        caseDocument.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        dartsDatabase.save(caseDocument);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            caseDocument,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        when(armDataManagementConfiguration.getCaseRecordClass()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getCaseRecordPropertiesFile()).thenReturn(
            "tests/arm/properties/all_properties/case-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        String prefix = String.format("%d_%d_1", armEod.getId(), caseDocument.getId());
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armEod.getId(), prefix);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals(prefix + ".a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual response {}", actualResponse);

        String expectedResponse = getContentsFromFile("tests/arm/service/testGenerateCaseArchiveRecord/all_properties/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<PREFIX>", prefix);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(armEod.getId()));
        expectedResponse = expectedResponse.replaceAll("<OBJECT_ID>", String.valueOf(caseDocument.getId()));
        expectedResponse = expectedResponse.replaceAll("<PARENT_ID>", String.valueOf(courtCaseEntity.getId()));
        expectedResponse = expectedResponse.replaceAll("<CASE_NUMBERS>", String.format("T%s", YESTERDAY.format(BASIC_ISO_DATE)) + "|Case1");
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_BY>", String.valueOf(uploadedBy.getId()));
        expectedResponse = expectedResponse.replaceAll("<UPLOADED_DATE_TIME>", caseDocument.getCreatedDateTime().format(formatter));

        log.info("expect response {}", expectedResponse);
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