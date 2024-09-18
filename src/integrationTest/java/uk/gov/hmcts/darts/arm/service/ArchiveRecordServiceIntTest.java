package uk.gov.hmcts.darts.arm.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.test.common.data.CourthouseTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.DatabaseDateSetter;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaTestData;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SPECIFIED_TIMES;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum.STANDARD;

@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports", "VariableDeclarationUsageDistance", "PMD.AssignmentInOperand"})
class ArchiveRecordServiceIntTest extends IntegrationBase {
    private static final OffsetDateTime YESTERDAY = now(UTC).minusDays(1).withHour(9).withMinute(0)
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

    @Autowired
    private DatabaseDateSetter dateConfigurer;

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
    void generateArchiveRecord_WithLiveMediaProperties_ReturnFileSuccess() throws Exception {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);
        OffsetDateTime endedAt = OffsetDateTime.of(2023, 9, 23, 13, 45, 0, 0, UTC);

        MediaEntity media = getMediaTestData().createMediaWith(
            hearing.getCourtroom(),
            startedAt,
            endedAt,
            1
        );
        MediaEntity savedMedia = dartsPersistence.save(media);

        savedMedia.setCourtroom(hearing.getCourtroom());
        savedMedia.setMediaFile("a-media-file.mp2");
        savedMedia.setCreatedDateTime(startedAt);
        savedMedia.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        savedMedia.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        savedMedia = dartsPersistence.save(savedMedia);


        hearing.addMedia(media);
        dartsDatabase.getHearingRepository().saveAndFlush(hearing);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData().someMinimal().getBuilder()
                .media(savedMedia)
                .status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
                .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
                .externalLocation(UUID.randomUUID())
                .updateRetention(true).build().getEntity();


        armEod.setTransferAttempts(1);
        armEod = dartsPersistence.save(armEod);

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
        expectedResponse = expectedResponse.replaceAll("<HEARING_DATE>", hearing.getHearingDate().atTime(0, 0, 0)
            .atZone(ZoneId.of("UTC")).format(formatter));
        expectedResponse = expectedResponse.replaceAll("<CREATE_DATE>", media.getCreatedDateTime().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", media.getStart().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", media.getEnd().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<COURTROOM>", media.getCourtroom().getName());
        expectedResponse = expectedResponse.replaceAll("<COURTHOUSE>", media.getCourtroom().getCourthouse().getDisplayName());

        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithLiveMediaPropertiesAndRetentionDate_ReturnFileSuccess() throws Exception {
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

        media.setMediaFile("a-media-file.mp2");
        media.setCreatedDateTime(startedAt);
        media.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        media.setRetConfScore(RETENTION_CONFIDENCE_SCORE);

        media = dartsPersistence.save(media);
        dartsDatabase.getHearingRepository().saveAndFlush(hearing);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData().someMinimal().getBuilder()
            .media(media)
            .status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID())
            .updateRetention(true).build().getEntity();

        hearing.addMedia(media);
        dartsDatabase.getHearingRepository().saveAndFlush(hearing);

        armEod.setTransferAttempts(1);
        armEod = dartsPersistence.save(armEod);

        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn("tests/arm/properties/media-record.properties");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getDateFormat()).thenReturn(DATE_FORMAT);
        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        String prefix = String.format("%d_%d_1", armEod.getId(), media.getId());
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armEod.getId(), prefix);

        log.info("Reading file {}", archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        Assertions.assertEquals(prefix + ".a360", archiveRecordFileInfo.getArchiveRecordFile().getName());

        String actualResponse = getFileContents(archiveRecordFileInfo.getArchiveRecordFile().getAbsoluteFile());
        log.info("actual response {}", actualResponse);

        String expectedResponse = getContentsFromFile("tests/arm/service/testGenerateMediaArchiveRecord/expectedResponse.a360");
        expectedResponse = expectedResponse.replaceAll("<PREFIX>", prefix);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(armEod.getId()));
        expectedResponse = expectedResponse.replaceAll("<OBJECT_ID>", String.valueOf(media.getId()));
        expectedResponse = expectedResponse.replaceAll("<PARENT_ID>", String.valueOf(media.getId()));
        expectedResponse = expectedResponse.replaceAll("<START_DATE_TIME>", media.getStart().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE_TIME>", media.getEnd().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<EVENT_DATE_TIME>", media.getRetainUntilTs().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<HEARING_DATE>", hearing.getHearingDate().atTime(0, 0, 0)
            .atZone(ZoneId.of("UTC")).format(formatter));
        expectedResponse = expectedResponse.replaceAll("<CREATE_DATE>", media.getCreatedDateTime().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", media.getStart().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", media.getEnd().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<COURTROOM>", media.getCourtroom().getName());
        expectedResponse = expectedResponse.replaceAll("<COURTHOUSE>", media.getCourtroom().getCourthouse().getDisplayName());

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
        MediaEntity savedMedia = dartsPersistence.save(media);
        savedMedia.setMediaFile("a-media-file.mp2");
        savedMedia.setCreatedDateTime(startedAt);
        savedMedia.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        savedMedia.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        savedMedia.setHearingList(new ArrayList<>(List.of(hearing)));

        savedMedia = dartsPersistence.save(savedMedia);

        hearing.addMedia(media);
        dartsDatabase.getHearingRepository().saveAndFlush(hearing);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData().someMinimal().getBuilder()
            .media(media)
            .status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID())
            .updateRetention(true).build().getEntity();

        armEod.setTransferAttempts(1);
        armEod = dartsPersistence.save(armEod);

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
        expectedResponse = expectedResponse.replaceAll("<HEARING_DATE>", hearing.getHearingDate().atTime(0, 0, 0)
            .atZone(ZoneId.of("UTC")).format(formatter));
        expectedResponse = expectedResponse.replaceAll("<CREATE_DATE>", media.getCreatedDateTime().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<START_DATE>", media.getStart().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<END_DATE>", media.getEnd().format(formatter));
        expectedResponse = expectedResponse.replaceAll("<COURTROOM>", media.getCourtroom().getName());
        expectedResponse = expectedResponse.replaceAll("<COURTHOUSE>", media.getCourtroom().getCourthouse().getDisplayName());

        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithLiveTranscriptionProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);

        authorisationStub.givenTestSchema();
        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var courtCase = authorisationStub.getCourtCaseEntity();
        courtCase.setCaseNumber("Case2");
        courtCase = dartsPersistence.save(courtCase);

        OffsetDateTime now = now(UTC);
        OffsetDateTime yesterday = now(UTC).minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimal().getBuilder()
            .createdBy(testUser).lastModifiedBy(testUser).requestedBy(testUser)
            .courtCases(new ArrayList<>(List.of(courtCase)))
            .hearings(new ArrayList<>(List.of(hearing)))
            .transcriptionStatus(dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(AWAITING_AUTHORISATION))
            .transcriptionUrgency(dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD))
            .transcriptionType(dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES))
            .isManualTranscription(true)
            .startTime(now)
            .endTime(yesterday)
            .transcriptionStatus(dartsDatabase.getTranscriptionStub()
                                     .getTranscriptionStatusByEnum(AWAITING_AUTHORISATION))
            .build();
        transcriptionEntity = dartsPersistence.save(transcriptionEntity);

        TranscriptionWorkflowEntity transcriptionWorkflowEntity = dartsPersistence.save(PersistableFactory.getTranscriptionWorkflowTestData()
                                  .someMinimal().getBuilder()
                                  .transcription(transcriptionEntity).build());

        transcriptionWorkflowEntity = dartsPersistence.save(transcriptionWorkflowEntity);
        TranscriptionCommentEntity commentEntity = dartsPersistence
            .save(PersistableFactory.getTranscriptionCommentTestData()
                                  .someMinimal().getBuilder()
                      .comment(REQUESTED_TRANSCRIPTION_COMMENT).transcription(transcriptionEntity).transcriptionWorkflow(transcriptionWorkflowEntity).build());


        transcriptionWorkflowEntity.setTranscriptionComments(
            new ArrayList<>(List.of(commentEntity)));
        transcriptionWorkflowEntity = dartsPersistence.save(transcriptionWorkflowEntity);

        transcriptionEntity.setTranscriptionWorkflowEntities(
            new ArrayList<>(List.of(transcriptionWorkflowEntity)));
        transcriptionEntity.setTranscriptionCommentEntities(
            new ArrayList<>(List.of(commentEntity)));
        transcriptionEntity = dartsPersistence.save(transcriptionEntity);

        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";

        TranscriptionDocumentEntity transcriptionDocumentEntity = PersistableFactory.getTranscriptionDocument().someMinimal()
            .getBuilder().transcription(transcriptionEntity).fileName(fileName).fileType(fileType)
            .fileSize(fileSize).uploadedBy(testUser).lastModifiedBy(testUser).checksum(checksum).uploadedDateTime(startedAt)
            .retConfScore(100).retConfReason("confidence reason")
            .build();
        transcriptionDocumentEntity.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        transcriptionDocumentEntity.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        transcriptionDocumentEntity = dartsPersistence.save(transcriptionDocumentEntity);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData().someMinimal().getBuilder()
            .transcriptionDocumentEntity(transcriptionDocumentEntity)
            .status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID())
            .media(null)
            .updateRetention(true).build().getEntity();
        armEod.setTransferAttempts(1);
        armEod = dartsPersistence.save(armEod);

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
    void generateArchiveRecord_WithLiveTranscriptionPropertiesRetentionDate_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);
        OffsetDateTime retainUntil = OffsetDateTime.of(2024, 9, 23, 13, 0, 0, 0, UTC);

        authorisationStub.givenTestSchema();
        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var courtCase = authorisationStub.getCourtCaseEntity();
        courtCase.setCaseNumber("Case2");
        dartsPersistence.save(courtCase);
        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimal().getBuilder()
            .createdBy(testUser).lastModifiedBy(testUser).requestedBy(testUser)
            .courtCases(new ArrayList<>(List.of(courtCase)))
            .hearings(new ArrayList<>(List.of(hearing)))
            .transcriptionStatus(dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(AWAITING_AUTHORISATION))
            .transcriptionUrgency(dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD))
            .transcriptionType(dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES))
            .isManualTranscription(true)
            .build();
        transcriptionEntity = dartsPersistence.save(transcriptionEntity);

        TranscriptionWorkflowEntity transcriptionWorkflowEntity = dartsPersistence.save(PersistableFactory.getTranscriptionWorkflowTestData()
                                                                                            .someMinimal().getBuilder()
                                                                                            .transcription(transcriptionEntity).build());

        TranscriptionCommentEntity commentEntity = dartsPersistence
            .save(PersistableFactory.getTranscriptionCommentTestData()
            .someMinimal().getBuilder().comment(REQUESTED_TRANSCRIPTION_COMMENT)
                      .transcription(transcriptionEntity).transcriptionWorkflow(transcriptionWorkflowEntity).build());


        transcriptionWorkflowEntity.setTranscriptionComments(
            new ArrayList<>(List.of(commentEntity)));
        transcriptionWorkflowEntity = dartsPersistence.save(transcriptionWorkflowEntity);

        transcriptionEntity.setTranscriptionWorkflowEntities(
            new ArrayList<>(List.of(transcriptionWorkflowEntity)));
        transcriptionEntity.setTranscriptionCommentEntities(
            new ArrayList<>(List.of(commentEntity)));
        transcriptionEntity = dartsPersistence.save(transcriptionEntity);

        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        TranscriptionDocumentEntity transcriptionDocumentEntity = PersistableFactory.getTranscriptionDocument().someMinimal()
            .getBuilder().transcription(transcriptionEntity).fileName(fileName).fileType(fileType)
            .fileSize(fileSize).uploadedBy(testUser).lastModifiedBy(testUser).checksum(checksum).uploadedDateTime(startedAt)
            .retConfScore(100).retConfReason("confidence reason")
            .build();
        transcriptionDocumentEntity.setRetainUntilTs(retainUntil);
        transcriptionDocumentEntity.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        transcriptionDocumentEntity.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        transcriptionDocumentEntity = dartsPersistence.save(transcriptionDocumentEntity);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData().someMinimal().getBuilder()
            .transcriptionDocumentEntity(transcriptionDocumentEntity)
            .status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID())
            .media(null)
            .updateRetention(true).build().getEntity();
        armEod.setTransferAttempts(1);
        armEod = dartsPersistence.save(armEod);

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
    void generateArchiveRecord_WithAllPropertiesTranscription_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);

        authorisationStub.givenTestSchema();
        var testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var courtCase = authorisationStub.getCourtCaseEntity();
        courtCase.setCaseNumber("Case2");
        dartsDatabase.getCaseRepository().save(courtCase);

        TranscriptionEntity transcriptionEntity = PersistableFactory.getTranscriptionTestData().someMinimal().getBuilder()
            .createdBy(testUser).lastModifiedBy(testUser).requestedBy(testUser)
            .courtCases(new ArrayList<>(List.of(courtCase)))
            .hearings(new ArrayList<>(List.of(hearing)))
            .transcriptionStatus(dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(AWAITING_AUTHORISATION))
            .transcriptionUrgency(dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(STANDARD))
            .transcriptionType(dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(SPECIFIED_TIMES))
            .isManualTranscription(true)
            .startTime(startedAt)
            .endTime(startedAt)
            .transcriptionStatus(dartsDatabase.getTranscriptionStub()
                                     .getTranscriptionStatusByEnum(AWAITING_AUTHORISATION))
            .build();

        transcriptionEntity = dartsPersistence.save(transcriptionEntity);

        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        TranscriptionDocumentEntity transcriptionDocumentEntity = PersistableFactory.getTranscriptionDocument().someMinimal()
            .getBuilder().transcription(transcriptionEntity).fileName(fileName).fileType(fileType)
            .fileSize(fileSize).uploadedBy(testUser).lastModifiedBy(testUser).checksum(checksum).uploadedDateTime(startedAt)
            .retConfScore(100).retConfReason("confidence reason")
            .build();
        transcriptionDocumentEntity.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        transcriptionDocumentEntity.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        transcriptionDocumentEntity = dartsPersistence.save(transcriptionDocumentEntity);

        TranscriptionWorkflowEntity transcriptionWorkflowEntity = dartsPersistence.save(PersistableFactory.getTranscriptionWorkflowTestData()
                                                                                            .someMinimal().getBuilder()
                                                                                            .transcription(transcriptionEntity).build());

        TranscriptionCommentEntity commentEntity = dartsPersistence
            .save(PersistableFactory.getTranscriptionCommentTestData()
                        .someMinimal()
                        .getBuilder().comment(REQUESTED_TRANSCRIPTION_COMMENT)
                        .transcription(transcriptionEntity).transcriptionWorkflow(transcriptionWorkflowEntity).build());


        transcriptionWorkflowEntity.setTranscriptionComments(new ArrayList<>(List.of(commentEntity)));
        transcriptionWorkflowEntity = dartsPersistence.save(transcriptionWorkflowEntity);

        transcriptionEntity.setTranscriptionWorkflowEntities(new ArrayList<>(List.of(transcriptionWorkflowEntity)));
        transcriptionEntity.setTranscriptionCommentEntities(new ArrayList<>(List.of(commentEntity)));
        transcriptionEntity = dartsPersistence.save(transcriptionEntity);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData().someMinimal().getBuilder()
            .transcriptionDocumentEntity(transcriptionDocumentEntity)
            .status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID())
            .media(null)
            .updateRetention(true).build().getEntity();
        armEod.setTransferAttempts(1);
        armEod = dartsPersistence.save(armEod);

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
    void generateArchiveRecord_WithLiveAnnotationProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);
        authorisationStub.givenTestSchema();

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        String testAnnotation = "TestAnnotation";
        AnnotationEntity annotation = PersistableFactory.getAnnotationTestData().someMinimal()
            .getBuilder().text(testAnnotation).currentOwner(testUser).hearingList(new ArrayList<>(List.of(hearing))).build();
        annotation = dartsPersistence.save(annotation);

        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = now();
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        AnnotationDocumentEntity annotationDocument = PersistableFactory.getAnnotationDocumentTestData().someMinimal().getBuilder()
            .annotation(annotation).fileType(fileType).fileName(fileName)
            .fileSize(fileSize).uploadedBy(testUser).uploadedDateTime(uploadedDateTime).checksum(checksum).build();

        annotationDocument.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        annotationDocument.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        annotationDocument = dartsPersistence.save(annotationDocument);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData().someMinimal().getBuilder()
            .annotationDocumentEntity(annotationDocument)
            .status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID())
            .media(null)
            .updateRetention(true).build().getEntity();
        armEod.setTransferAttempts(1);
        armEod = dartsPersistence.save(armEod);

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
        expectedResponse = expectedResponse.replaceAll("<USER_ID>", testUser.getId().toString());

        log.info("expect response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithLiveAnnotationPropertiesRetentionDate_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        OffsetDateTime retainUntil = OffsetDateTime.of(2024, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);
        authorisationStub.givenTestSchema();

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        String testAnnotation = "TestAnnotation";
        AnnotationEntity annotation = PersistableFactory.getAnnotationTestData().someMinimal()
            .getBuilder().text(testAnnotation).currentOwner(testUser).hearingList(new ArrayList<>(List.of(hearing))).build();
        annotation = dartsPersistence.save(annotation);

        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = now();
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        AnnotationDocumentEntity annotationDocument = PersistableFactory
            .getAnnotationDocumentTestData().someMinimal().getBuilder()
            .annotation(annotation).retainUntilTs(retainUntil)
            .fileType(fileType).fileName(fileName).fileSize(fileSize).uploadedBy(testUser).uploadedDateTime(uploadedDateTime).checksum(checksum).build();

        annotationDocument.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        annotationDocument.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        annotationDocument = dartsPersistence.save(annotationDocument);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData().someMinimal().getBuilder()
            .annotationDocumentEntity(annotationDocument)
            .status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID())
            .media(null)
            .updateRetention(true).build().getEntity();
        armEod.setTransferAttempts(1);
        armEod = dartsPersistence.save(armEod);

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
        expectedResponse = expectedResponse.replaceAll("<USER_ID>", testUser.getId().toString());

        log.info("expect response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithAllAnnotationProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);
        authorisationStub.givenTestSchema();

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        String testAnnotation = "TestAnnotation";
        AnnotationEntity annotation = PersistableFactory.getAnnotationTestData().someMinimal()
            .getBuilder().currentOwner(testUser).text(testAnnotation)
                .hearingList(new ArrayList<>(List.of(hearing))).build();
        annotation = dartsPersistence.save(annotation);

        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = now();
        final String checksum = "C3CCA7021CF79B42F245AF350601C284";
        AnnotationDocumentEntity annotationDocument = PersistableFactory.getAnnotationDocumentTestData().someMinimal().getBuilder()
            .annotation(annotation).fileName(fileName)
            .fileType(fileType).fileSize(fileSize).uploadedBy(testUser)
            .lastModifiedBy(testUser).checksum(checksum)
            .uploadedDateTime(uploadedDateTime).build();
        annotationDocument.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        annotationDocument.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        annotationDocument = dartsPersistence.save(annotationDocument);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData().someMinimal().getBuilder()
            .annotationDocumentEntity(annotationDocument)
            .status(dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID())
            .media(null)
            .updateRetention(true).build().getEntity();
        armEod.setTransferAttempts(1);
        armEod = dartsPersistence.save(armEod);

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
        expectedResponse = expectedResponse.replaceAll("<USER_ID>", testUser.getId().toString());

        log.info("expect response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithLiveCaseProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);

        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimal().getBuilder()
            .caseNumber("Case1").courthouse(CourthouseTestData.someMinimalCourthouse()).build();
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        courtCaseEntity = dartsPersistence.save(courtCaseEntity);

        CaseDocumentEntity caseDocument = PersistableFactory.getCaseDocumentTestData()
            .someMinimal().getBuilder().courtCase(courtCaseEntity)
            .lastModifiedBy(uploadedBy).checksum("xC3CCA7021CF79B42F245AF350601C284").fileType("docx").build();

        caseDocument = dartsPersistence.save(caseDocument);
        caseDocument.setFileName("test_case_document.docx");
        caseDocument.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        caseDocument.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        caseDocument = dartsPersistence.save(caseDocument);

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
        expectedResponse = expectedResponse.replaceAll("<COURTHOUSE>", courtCaseEntity.getCourthouse().getDisplayName());

        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithLiveCasePropertiesRetentionDate_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        OffsetDateTime retainUntil = OffsetDateTime.of(2024, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);

        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimal().getBuilder()
            .caseNumber("Case1").courthouse(CourthouseTestData.someMinimalCourthouse()).build();
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        courtCaseEntity = dartsPersistence.save(courtCaseEntity);

        CaseDocumentEntity caseDocument = PersistableFactory.getCaseDocumentTestData()
            .someMinimal().getBuilder().courtCase(courtCaseEntity)
            .lastModifiedBy(uploadedBy).checksum("xC3CCA7021CF79B42F245AF350601C284").fileType("docx").build();

        caseDocument = dartsPersistence.save(caseDocument);

        caseDocument.setFileName("test_case_document.docx");
        caseDocument.setRetainUntilTs(retainUntil);
        caseDocument.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        caseDocument.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        caseDocument = dartsPersistence.save(caseDocument);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData().someMinimal()
            .getBuilder().caseDocument(caseDocument).status(dartsDatabase.getExternalObjectDirectoryStub().getStatus(ARM_DROP_ZONE))
            .media(null).externalLocationType(dartsDatabase.getExternalObjectDirectoryStub().getLocation(ARM)).externalLocation(UUID.randomUUID()).build();

        armEod.setTransferAttempts(1);
        armEod = dartsPersistence.save(armEod);

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
        expectedResponse = expectedResponse.replaceAll("<COURTHOUSE>", courtCaseEntity.getCourthouse().getDisplayName());

        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void generateArchiveRecord_WithAllCaseProperties_ReturnFileSuccess() throws IOException {
        OffsetDateTime startedAt = OffsetDateTime.of(2023, 9, 23, 13, 0, 0, 0, UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(startedAt);

        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimal().getBuilder()
            .caseNumber("Case1").courthouse(CourthouseTestData.someMinimalCourthouse()).build();

        courtCaseEntity = dartsPersistence.save(courtCaseEntity);
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        CaseDocumentEntity caseDocument = PersistableFactory.getCaseDocumentTestData()
            .someMinimal().getBuilder().courtCase(courtCaseEntity)
            .lastModifiedBy(uploadedBy).checksum("xC3CCA7021CF79B42F245AF350601C284").fileType("docx").build();
        caseDocument.setRetConfReason(RETENTION_CONFIDENCE_REASON);
        caseDocument.setRetConfScore(RETENTION_CONFIDENCE_SCORE);
        caseDocument = dartsPersistence.save(caseDocument);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData().someMinimal()
            .getBuilder().caseDocument(caseDocument).status(dartsDatabase.getExternalObjectDirectoryStub().getStatus(ARM_DROP_ZONE))
            .media(null).externalLocationType(dartsDatabase.getExternalObjectDirectoryStub().getLocation(ARM)).externalLocation(UUID.randomUUID()).build();

        armEod.setTransferAttempts(1);
        armEod = dartsPersistence.save(armEod);

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
        expectedResponse = expectedResponse.replaceAll("<COURTHOUSE>", courtCaseEntity.getCourthouse().getDisplayName());
        expectedResponse = expectedResponse.replaceAll("<TITLE>", caseDocument.getFileName());

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