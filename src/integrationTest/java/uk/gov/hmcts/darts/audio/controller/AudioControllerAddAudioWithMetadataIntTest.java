package uk.gov.hmcts.darts.audio.controller;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.unit.DataSize;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.Problem;
import uk.gov.hmcts.darts.audio.service.AudioAsyncService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.DataGenerator;
import uk.gov.hmcts.darts.test.common.LogUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static ch.qos.logback.classic.Level.toLevel;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.servlet.multipart.max-file-size=4MB",
    "spring.servlet.multipart.max-request-size=4MB",
})
class AudioControllerAddAudioWithMetadataIntTest extends IntegrationBase {

    @Value("${local.server.port}")
    protected int port;

    private static final URI ENDPOINT = URI.create("/audios");
    private static final OffsetDateTime STARTED_AT = OffsetDateTime.of(2024, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final Path AUDIO_BINARY_PAYLOAD_1 = DataGenerator.createUniqueFile(DataSize.ofBytes(10),
                                                                                      DataGenerator.FileType.MP2);
    private static final Path AUDIO_BINARY_PAYLOAD_2 = DataGenerator.createUniqueFile(DataSize.ofBytes(10),
                                                                                      DataGenerator.FileType.MP2);
    private static final Path AUDIO_BINARY_PAYLOAD_3 = DataGenerator.createUniqueFile(DataSize.ofBytes(10),
                                                                                      DataGenerator.FileType.MP2);
    private static final Path AUDIO_BINARY_PAYLOAD_EXCEEDING_MAX_ALLOWABLE_SIZE = DataGenerator.createUniqueFile(DataSize.ofMegabytes(5),
                                                                                                                 DataGenerator.FileType.MP2);

    @Value("${darts.audio.max-file-duration}")
    private Duration maxFileDuration;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthorisationStub authorisationStub;
    @Autowired
    private EventStub eventStub;
    @Autowired
    HearingStub hearingStub;
    @MockBean
    private UserIdentity mockUserIdentity;

    @MockBean
    AudioAsyncService audioAsyncService;

    @Value("${spring.servlet.multipart.max-file-size}")
    private DataSize addAudioThreshold;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;


    @BeforeEach
    void beforeEach() {
        openInViewUtil.openEntityManager();
        authorisationStub.givenTestSchema();

        UserAccountEntity testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        dartsDatabase.getUserAccountRepository().save(testUser);

        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        HearingEntity hearingForEvent = hearingStub.createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingForEvent, 10, STARTED_AT.minusMinutes(20), "LOG");
        HearingEntity hearingDifferentCourtroom = hearingStub.createHearing("Bristol", "2", "case2", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingDifferentCourtroom, 10, STARTED_AT.minusMinutes(20), "LOG");
    }

    @AfterEach
    void closeHibernateSession() {
        openInViewUtil.closeEntityManager();
    }

    @Test
    void addAudioMetadata() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(AUDIO_BINARY_PAYLOAD_1))
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isOk())
            .andReturn();

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        List<HearingEntity> addAudioLinkedHearings = new ArrayList<>();
        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(3, addAudioLinkedHearings.size());

        for (HearingEntity hearing : addAudioLinkedHearings) {
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearing.getId());
            MediaEntity media = mediaEntities.get(0);
            assertEquals(1, mediaEntities.size());
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(STARTED_AT.plus(maxFileDuration), media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            List<MediaLinkedCaseEntity> mediaLinkedCaseEntities = dartsDatabase.getMediaLinkedCaseRepository().findByMedia(media);
            assertEquals(3, mediaLinkedCaseEntities.size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(media.getCourtroom().getId()).get().getName());
            assertEquals(media.getId().toString(), media.getChronicleId());
            assertEquals(true, media.getIsCurrent());
            assertNull(media.getAntecedentId());
        }

        List<HearingEntity> hearingsInAnotherCourtroom = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate(
            "bristol",
            "2",
            STARTED_AT.toLocalDate()
        );
        assertEquals(1, hearingsInAnotherCourtroom.size());//should have hearingDifferentCourtroom

        HearingEntity hearingEntity = hearingsInAnotherCourtroom.get(0);
        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearingEntity.getId());
        assertEquals(0, mediaEntities.size());//shouldn't have any as no audio in that courtroom
    }

    @Test
    void addAudioMetadata_courtHouseNotFound_404ShouldBeReturned() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1");
        addAudioMetadataRequest.setCourthouse("UNKNOWN_COURTHOUSE");
        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(AUDIO_BINARY_PAYLOAD_1))
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        MvcResult response = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isNotFound())
            .andReturn();
        String content = response.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(CommonApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST.getType(), problemResponse.getType());
    }

    @Test
    void addAudioMetadataDuplicate() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        makeAddAudioCall();
        makeAddAudioCall();

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        List<HearingEntity> addAudioLinkedHearings = new ArrayList<>();
        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(3, addAudioLinkedHearings.size());

        for (HearingEntity hearing : addAudioLinkedHearings) {
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearing.getId());
            MediaEntity media = mediaEntities.get(0);
            assertEquals(1, mediaEntities.size());
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(STARTED_AT, media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            List<MediaLinkedCaseEntity> mediaLinkedCaseEntities = dartsDatabase.getMediaLinkedCaseRepository().findByMedia(media);
            assertEquals(3, mediaLinkedCaseEntities.size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(media.getCourtroom().getId()).get().getName());
            assertEquals(media.getId().toString(), media.getChronicleId());
            assertNull(media.getAntecedentId());
            assertEquals(media.getId().toString(), media.getChronicleId());
        }

        List<HearingEntity> hearingsInAnotherCourtroom = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate(
            "bristol",
            "2",
            STARTED_AT.toLocalDate()
        );
        assertEquals(1, hearingsInAnotherCourtroom.size());//should have hearingDifferentCourtroom

        HearingEntity hearingEntity = hearingsInAnotherCourtroom.get(0);
        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearingEntity.getId());
        assertEquals(0, mediaEntities.size());//shouldn't have any as no audio in that courtroom

        assertFalse(Objects.requireNonNull(LogUtil.getMemoryLogger())
                        .searchLogs("Exact duplicate detected based upon media metadata and checksum.", toLevel(
                            Level.INFO_INT)).isEmpty());
    }

    @Test
    void addAudioMetadataVersionedDueToDuplicateMetadataButDifferentChecksum() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        makeAddAudioCall();

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository()
            .findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        List<HearingEntity> addAudioLinkedHearings = new ArrayList<>();
        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(3, addAudioLinkedHearings.size());

        MediaEntity originalMedia = null;
        for (HearingEntity hearing : addAudioLinkedHearings) {
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearing.getId());

            assertEquals(1, mediaEntities.size());

            originalMedia = mediaEntities.get(0);
            assertEquals(STARTED_AT, originalMedia.getStart());
            assertEquals(STARTED_AT, originalMedia.getEnd());
            assertEquals(1, originalMedia.getChannel());
            assertEquals(2, originalMedia.getTotalChannels());
            List<MediaLinkedCaseEntity> mediaLinkedCaseEntities = dartsDatabase.getMediaLinkedCaseRepository().findByMedia(originalMedia);
            assertEquals(3, mediaLinkedCaseEntities.size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(originalMedia.getCourtroom().getId()).get().getName());
            assertEquals(originalMedia.getId().toString(), originalMedia.getChronicleId());
            assertNull(originalMedia.getAntecedentId());
        }

        List<HearingEntity> hearingsInAnotherCourtroom = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate(
            "bristol",
            "2",
            STARTED_AT.toLocalDate()
        );
        assertEquals(1, hearingsInAnotherCourtroom.size());//should have hearingDifferentCourtroom

        Integer newMedia = uploadAnotherAudioWithSize(AUDIO_BINARY_PAYLOAD_2, originalMedia.getId().toString(), originalMedia.getId().toString());
        Integer newMedia2 = uploadAnotherAudioWithSize(AUDIO_BINARY_PAYLOAD_3, newMedia.toString(), originalMedia.getId().toString());
        assertNotEquals(newMedia, newMedia2);
        Optional<MediaEntity> newMediaEntity = dartsDatabase.getMediaRepository().findById(newMedia);
        assertEquals(false, newMediaEntity.get().getIsCurrent());
        Optional<MediaEntity> newMedia2Entity = dartsDatabase.getMediaRepository().findById(newMedia2);
        assertEquals(true, newMedia2Entity.get().getIsCurrent());

    }

    @Test
    void addAudioBeyondAudioFileSizeThresholdExceeded() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        MvcResult mvcResult = makeAddAudioCall(AUDIO_BINARY_PAYLOAD_EXCEEDING_MAX_ALLOWABLE_SIZE)
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        String expectedResponse = """
            {
              "type": "AUDIO_108",
              "title": "The audio metadata size exceeds maximum threshold",
              "status": 400
            }
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void addAudioMetadataNonExistingCourthouse() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "TEST", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(AUDIO_BINARY_PAYLOAD_1))
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        MvcResult mvcResult = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isNotFound())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"type":"COMMON_100","title":"Provided courthouse does not exist","status":404,"detail":"Courthouse 'TEST' not found."}""";

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void addAudioUnsupportedType() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        String unknownType = "unsupportedType";
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1", unknownType);

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(AUDIO_BINARY_PAYLOAD_1))
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        MvcResult mvcResult = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(problem.getType(), AudioApiError.UNEXPECTED_FILE_TYPE.getType());
        assertEquals(problem.getTitle(), AudioApiError.UNEXPECTED_FILE_TYPE.getTitle());
    }

    @Test
    void addAudioNotProvided() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            "".getBytes()
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        MvcResult mvcResult = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(AudioApiError.AUDIO_NOT_PROVIDED.getType(), problem.getType());
        assertEquals(AudioApiError.AUDIO_NOT_PROVIDED.getTitle(), problem.getTitle());
    }

    @Test
    void addAudioDurationOutOfBounds() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration).plusSeconds(1), "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(AUDIO_BINARY_PAYLOAD_EXCEEDING_MAX_ALLOWABLE_SIZE))
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        MvcResult mvcResult = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(AudioApiError.FILE_DURATION_OUT_OF_BOUNDS.getType(), problem.getType());
        assertEquals(AudioApiError.FILE_DURATION_OUT_OF_BOUNDS.getTitle(), problem.getTitle());
    }

    @Test
    void addFailedToUploadAudio() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1");

        // create an audio file that throws an exception
        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(AUDIO_BINARY_PAYLOAD_1))
        ) {
            private int fileSignatureValidationCallCount;

            @Override
            public InputStream getInputStream() throws IOException {
                // fail on any call other than for the file signature validation
                if (fileSignatureValidationCallCount != 0) {
                    throw new IOException();
                }
                fileSignatureValidationCallCount = fileSignatureValidationCallCount + 1;
                return super.getInputStream();
            }
        };

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        MvcResult mvcResult = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(AudioApiError.FAILED_TO_UPLOAD_AUDIO_FILE.getType(), problem.getType());
        assertEquals(AudioApiError.FAILED_TO_UPLOAD_AUDIO_FILE.getTitle(), problem.getTitle());
    }

    @Test
    void addAudioSizeOutsideOfBoundaries() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1");

        // set the file size to be greater than the maximum threshold
        addAudioMetadataRequest.setFileSize(addAudioThreshold.toBytes() + 1);

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(AUDIO_BINARY_PAYLOAD_1))
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        MvcResult mvcResult = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(AudioApiError.FILE_SIZE_OUT_OF_BOUNDS.getType(), problem.getType());
        assertEquals(AudioApiError.FILE_SIZE_OUT_OF_BOUNDS.getTitle(), problem.getTitle());
    }

    @Test
    void addAudioFileExtensionIncorrect() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.incorrect",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(AUDIO_BINARY_PAYLOAD_1))
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        MvcResult mvcResult = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(AudioApiError.UNEXPECTED_FILE_TYPE.getType(), problem.getType());
        assertEquals(AudioApiError.UNEXPECTED_FILE_TYPE.getTitle(), problem.getTitle());
    }

    @Test
    void addAudioFileExtensionContentType() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpegincorrect",
            IOUtils.toByteArray(Files.newInputStream(AUDIO_BINARY_PAYLOAD_1))
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        MvcResult mvcResult = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(AudioApiError.UNEXPECTED_FILE_TYPE.getType(), problem.getType());
        assertEquals(AudioApiError.UNEXPECTED_FILE_TYPE.getTitle(), problem.getTitle());
    }

    @Test
    void addAudioFileSignatureException() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            "Not an mp2 signature".getBytes()
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        MvcResult mvcResult = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(AudioApiError.UNEXPECTED_FILE_TYPE.getType(), problem.getType());
        assertEquals(AudioApiError.UNEXPECTED_FILE_TYPE.getTitle(), problem.getTitle());
    }

    @Test
    void addAudioReturnForbiddenError() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.DAR_PC);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "TEST", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(AUDIO_BINARY_PAYLOAD_1))
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        MvcResult mvcResult = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(audioFile)
                    .file(metadataJson))
            .andExpect(status().isForbidden())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"AUTHORISATION_109","title":"User is not authorised for this endpoint","status":403}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt,
                                                          OffsetDateTime endedAt, String courthouse, String courtroom, String filetype) throws IOException {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom, filetype,
                                     AUDIO_BINARY_PAYLOAD_1, "case1", "case2", "case3");
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt,
                                                          OffsetDateTime endedAt, String courthouse, String courtroom) throws IOException {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom,
                                     "mp2", AUDIO_BINARY_PAYLOAD_1, "case1", "case2", "case3");
    }


    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt,
                                                          OffsetDateTime endedAt,
                                                          String courthouse, String courtroom, Path audioBinaryPayload) throws IOException {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom, "mp2", audioBinaryPayload, "case1", "case2", "case3");
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt,
                                                          String courthouse, String courtroom, Path audioBinaryPayload,
                                                          String... casesList) throws IOException {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom,
                                     "mp2", audioBinaryPayload, casesList);
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt,
                                                          String courthouse, String courtroom, String filetype, Path audioBinaryPayload,
                                                          String... casesList) throws IOException {

        AddAudioMetadataRequest addAudioMetadataRequest = new AddAudioMetadataRequest();
        addAudioMetadataRequest.startedAt(startedAt);
        addAudioMetadataRequest.endedAt(endedAt);
        addAudioMetadataRequest.setChannel(1);
        addAudioMetadataRequest.totalChannels(2);
        addAudioMetadataRequest.format(filetype);
        addAudioMetadataRequest.filename("test");
        addAudioMetadataRequest.courthouse(courthouse);
        addAudioMetadataRequest.courtroom(courtroom);
        addAudioMetadataRequest.cases(List.of(casesList));
        addAudioMetadataRequest.setMediaFile("media file");
        addAudioMetadataRequest.setFileSize(Files.size(audioBinaryPayload));
        addAudioMetadataRequest.setChecksum("calculatedchecksum");
        return addAudioMetadataRequest;
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
    private ResultActions makeAddAudioCall() throws Exception {
        return makeAddAudioCall(AUDIO_BINARY_PAYLOAD_1);
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
    private ResultActions makeAddAudioCall(Path audioBinaryPayload, String... casesToMapTo) throws Exception {
        UserAccountEntity testUser = authorisationStub.getSystemUser();
        dartsDatabase.getUserAccountRepository().save(testUser);

        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        HearingEntity hearingForEvent = hearingStub.createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingForEvent, 10, STARTED_AT.minusMinutes(20), "LOG");
        HearingEntity hearingDifferentCourtroom = hearingStub.createHearing("Bristol", "2", "case2", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingDifferentCourtroom, 10, STARTED_AT.minusMinutes(20), "LOG");
        HearingEntity hearingAfter = hearingStub.createHearing("Bristol", "1", "case3", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingAfter, 10, STARTED_AT.plusMinutes(20), "LOG");

        AddAudioMetadataRequest addAudioMetadataRequest;
        if (casesToMapTo.length == 0) {
            addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT, "Bristol", "1", audioBinaryPayload);
        } else {
            addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT, "Bristol", "1", audioBinaryPayload, casesToMapTo);
        }

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(audioBinaryPayload)));

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        return mockMvc.perform(
            multipart(ENDPOINT)
                .file(audioFile)
                .file(metadataJson));
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
    private Integer uploadAnotherAudioWithSize(Path audioBinaryPayload, String expectedAntecedantId, String expectedChronicleId) throws Exception {
        makeAddAudioCall(audioBinaryPayload)
            .andExpect(status().isOk());

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        List<HearingEntity> addAudioLinkedHearings = new ArrayList<>();
        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(3, addAudioLinkedHearings.size());

        MediaEntity media = null;
        for (HearingEntity hearing : addAudioLinkedHearings) {
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearing.getId());

            assertEquals(1, mediaEntities.size());

            media = mediaEntities.get(0);
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(STARTED_AT, media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            List<MediaLinkedCaseEntity> mediaLinkedCaseEntities = dartsDatabase.getMediaLinkedCaseRepository().findByMedia(media);
            assertEquals(3, mediaLinkedCaseEntities.size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(media.getCourtroom().getId()).get().getName());
            assertEquals(expectedChronicleId, media.getChronicleId());
            assertEquals(expectedAntecedantId, media.getAntecedentId());
        }

        List<HearingEntity> hearingsInAnotherCourtroom = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate(
            "bristol",
            "2",
            STARTED_AT.toLocalDate()
        );
        assertEquals(1, hearingsInAnotherCourtroom.size());//should have hearingDifferentCourtroom
        HearingEntity hearingEntity = hearingsInAnotherCourtroom.get(0);
        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearingEntity.getId());
        assertEquals(0, mediaEntities.size());//shouldn't have any as no audio in that courtroom
        assertFalse(Objects.requireNonNull(LogUtil.getMemoryLogger())
                        .searchLogs("Revised version of media added", toLevel(
                            Level.INFO_INT)).isEmpty());

        return media.getId();
    }

}