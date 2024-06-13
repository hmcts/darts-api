package uk.gov.hmcts.darts.audio.controller;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.Problem;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.InMemoryMultipart;
import uk.gov.hmcts.darts.test.common.LogUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static ch.qos.logback.classic.Level.toLevel;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AudioControllerAddAudioMetadataIntTest extends IntegrationBase {

    @Value("${local.server.port}")
    protected int port;

    private static final URI ENDPOINT = URI.create("/audios");
    private static final OffsetDateTime STARTED_AT = OffsetDateTime.of(2024, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);

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

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.servlet.multipart.max-file-size}")
    private DataSize addAudioThreshold;

    @BeforeEach
    void beforeEach() {
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

    @Test
    void addAudioMetadata() throws Exception {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(Path.of(Thread.currentThread().getContextClassLoader()
                                                                 .getResource("sample6.mp2").getFile())))
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
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearing.getId());
            MediaEntity media = mediaEntities.get(0);
            assertEquals(1, mediaEntities.size());
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(STARTED_AT.plus(maxFileDuration), media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            assertEquals(3, media.getCaseNumberList().size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(media.getCourtroom().getId()).get().getName());
            assertEquals(media.getId().toString(), media.getChronicleId());
            assertNull(media.getAntecedentId());
        }

        List<HearingEntity> hearingsInAnotherCourtroom = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate(
            "bristol",
            "2",
            STARTED_AT.toLocalDate()
        );
        assertEquals(1, hearingsInAnotherCourtroom.size());//should have hearingDifferentCourtroom

        HearingEntity hearingEntity = hearingsInAnotherCourtroom.get(0);
        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearingEntity.getId());
        assertEquals(0, mediaEntities.size());//shouldn't have any as no audio in that courtroom
    }

    @Test
    void addAudioMetadataDifferentCases() throws Exception {
        makeAddAudioCall(1000, "case1");

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        List<HearingEntity> addAudioLinkedHearings = new ArrayList<>();
        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case1")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(1, addAudioLinkedHearings.size());

        MediaEntity mediaFirst = null;
        for (HearingEntity hearing : addAudioLinkedHearings) {
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearing.getId());
            mediaFirst = mediaEntities.get(0);
            assertEquals(1, mediaEntities.size());
            assertEquals(STARTED_AT, mediaFirst.getStart());
            assertEquals(STARTED_AT, mediaFirst.getEnd());
            assertEquals(1, mediaFirst.getChannel());
            assertEquals(2, mediaFirst.getTotalChannels());
            assertEquals(1, mediaFirst.getCaseNumberList().size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(mediaFirst.getCourtroom().getId()).get().getName());
            assertEquals(mediaFirst.getId().toString(), mediaFirst.getChronicleId());
            assertNull(mediaFirst.getAntecedentId());
        }

        makeAddAudioCall(1000, "case2");

        addAudioLinkedHearings = new ArrayList<>();
        allHearings = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case2")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(1, addAudioLinkedHearings.size());

        MediaEntity mediaSecond = null;
        for (HearingEntity hearing : addAudioLinkedHearings) {
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearing.getId());
            mediaSecond = mediaEntities.get(0);
            assertEquals(1, mediaEntities.size());
            assertEquals(STARTED_AT, mediaSecond.getStart());
            assertEquals(STARTED_AT, mediaSecond.getEnd());
            assertEquals(1, mediaSecond.getChannel());
            assertEquals(2, mediaSecond.getTotalChannels());
            assertEquals(1, mediaSecond.getCaseNumberList().size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(mediaSecond.getCourtroom().getId()).get().getName());
            assertEquals(mediaSecond.getId().toString(), mediaSecond.getChronicleId());
            assertNull(mediaSecond.getAntecedentId());
        }

        assertNotSame(mediaFirst, mediaSecond);
    }

    @Test
    void addAudioMetadataDuplicate() throws Exception {
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
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearing.getId());
            MediaEntity media = mediaEntities.get(0);
            assertEquals(1, mediaEntities.size());
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(STARTED_AT, media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            assertEquals(3, media.getCaseNumberList().size());
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
        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearingEntity.getId());
        assertEquals(0, mediaEntities.size());//shouldn't have any as no audio in that courtroom

        assertFalse(Objects.requireNonNull(LogUtil.getMemoryLogger())
                        .searchLogs("Duplicate audio upload detected", toLevel(
                            Level.INFO_INT)).isEmpty());
    }

    @Test
    void addAudioMetadataVersionedDueToDuplicateFileSizeDifference() throws Exception {
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
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearing.getId());

            assertEquals(1, mediaEntities.size());

            originalMedia = mediaEntities.get(0);
            assertEquals(STARTED_AT, originalMedia.getStart());
            assertEquals(STARTED_AT, originalMedia.getEnd());
            assertEquals(1, originalMedia.getChannel());
            assertEquals(2, originalMedia.getTotalChannels());
            assertEquals(3, originalMedia.getCaseNumberList().size());
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

        Integer newMedia = uploadAnotherAudioWithSize(1001L, originalMedia.getId().toString(), originalMedia.getId().toString());
        Integer newMedia2 = uploadAnotherAudioWithSize(1002L, newMedia.toString(), originalMedia.getId().toString());
        assertNotEquals(newMedia, newMedia2);
    }

    @Test
    void addAudioBeyondAudioFileSizeThresholdExceeded() throws Exception {
        InMemoryMultipart audioFile = InMemoryMultipart.getMultiPartOfRandomisedLengthKb(
            "file",
            "audio.mp2",
            // add one onto the threshold so we are going to fail
            addAudioThreshold.toBytes() + 1
        );

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

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plusMinutes(20), "Bristol", "1");

        try {
            streamFileWithMetaData(audioFile, addAudioMetadataRequest, "http://localhost:" + port + "/audios");
            Assertions.fail();
        } catch (RestClientException restClientException) {
            String expectedJson = """
                400 : "{"title":"Bad Request","status":400,"detail":"Maximum upload size exceeded"}"
                """;

            assertEquals(expectedJson.trim(), restClientException.getMessage());
        }
    }

    @Test
    void addAudioMetadataNonExistingCourthouse() throws Exception {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "TEST", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(Path.of(Thread.currentThread().getContextClassLoader()
                                                                 .getResource("sample6.mp2").getFile())))
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
        String expectedJson = """
            {"type":"COMMON_100","title":"Provided courthouse does not exist","status":400,"detail":"Courthouse 'TEST' not found."}""";

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void addAudioUnsupportedType() throws Exception {
        String unknownType = "unsupportedType";
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1", unknownType);

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(Path.of(Thread.currentThread().getContextClassLoader()
                                                                 .getResource("sample6.mp2").getFile())))
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
        assertEquals(problem.getType().toString(), AudioApiError.UNEXPECTED_FILE_TYPE.getType().toString());
        assertEquals(problem.getTitle(), AudioApiError.UNEXPECTED_FILE_TYPE.getTitle());
    }

    @Test
    void addAudioNotProvided() throws Exception {
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
        assertEquals(AudioApiError.AUDIO_NOT_PROVIDED.getType().toString(), problem.getType().toString());
        assertEquals(AudioApiError.AUDIO_NOT_PROVIDED.getTitle(), problem.getTitle());
    }

    @Test
    void addAudioDurationOutOfBounds() throws Exception {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration).plusSeconds(1), "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(Path.of(Thread.currentThread().getContextClassLoader()
                                                                 .getResource("sample6.mp2").getFile())))
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
        assertEquals(AudioApiError.FILE_DURATION_OUT_OF_BOUNDS.getType().toString(), problem.getType().toString());
        assertEquals(AudioApiError.FILE_DURATION_OUT_OF_BOUNDS.getTitle(), problem.getTitle());
    }

    @Test
    void addFailedToUploadAudio() throws Exception {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1");

        // create an audio file that throws an exception
        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(Path.of(Thread.currentThread().getContextClassLoader()
                                                                 .getResource("sample6.mp2").getFile())))
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
        assertEquals(AudioApiError.FAILED_TO_UPLOAD_AUDIO_FILE.getType().toString(), problem.getType().toString());
        assertEquals(AudioApiError.FAILED_TO_UPLOAD_AUDIO_FILE.getTitle(), problem.getTitle());
    }

    @Test
    void addAudioSizeOutsideOfBoundaries() throws Exception {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1");

        // set the file size to be greater than the maximum threshold
        addAudioMetadataRequest.setFileSize(addAudioThreshold.toBytes() + 1);

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(Path.of(Thread.currentThread().getContextClassLoader()
                                                                 .getResource("sample6.mp2").getFile())))
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
        assertEquals(AudioApiError.FILE_SIZE_OUT_OF_BOUNDS.getType().toString(), problem.getType().toString());
        assertEquals(AudioApiError.FILE_SIZE_OUT_OF_BOUNDS.getTitle(), problem.getTitle());
    }

    @Test
    void addAudioFileExtensionIncorrect() throws Exception {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.incorrect",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(Path.of(Thread.currentThread().getContextClassLoader()
                                                                 .getResource("sample6.mp2").getFile())))
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
        assertEquals(AudioApiError.UNEXPECTED_FILE_TYPE.getType().toString(), problem.getType().toString());
        assertEquals(AudioApiError.UNEXPECTED_FILE_TYPE.getTitle(), problem.getTitle());
    }

    @Test
    void addAudioFileExtensionContentType() throws Exception {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plus(maxFileDuration), "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpegincorrect",
            IOUtils.toByteArray(Files.newInputStream(Path.of(Thread.currentThread().getContextClassLoader()
                                                                 .getResource("sample6.mp2").getFile())))
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
        assertEquals(AudioApiError.UNEXPECTED_FILE_TYPE.getType().toString(), problem.getType().toString());
        assertEquals(AudioApiError.UNEXPECTED_FILE_TYPE.getTitle(), problem.getTitle());
    }

    @Test
    void addAudioFileSignatureException() throws Exception {
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
        assertEquals(AudioApiError.UNEXPECTED_FILE_TYPE.getType().toString(), problem.getType().toString());
        assertEquals(AudioApiError.UNEXPECTED_FILE_TYPE.getTitle(), problem.getTitle());
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt,
                                                          OffsetDateTime endedAt, String courthouse, String courtroom, String filetype) {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom, filetype,
                                     100, "case1", "case2", "case3");
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt,
                                                          OffsetDateTime endedAt, String courthouse, String courtroom) {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom,
                                     "mp2", 100, "case1", "case2", "case3");
    }


    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt,
                                                          OffsetDateTime endedAt,
                                                          String courthouse, String courtroom, long fileSize) {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom, "mp2", fileSize, "case1", "case2", "case3");
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt,
                                                          String courthouse, String courtroom, long fileSize, String... casesList) {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom,
                                     "mp2", fileSize, casesList);

    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt,
                                                          String courthouse, String courtroom, String filetype, long fileSize, String... casesList) {
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
        addAudioMetadataRequest.setFileSize(fileSize);
        addAudioMetadataRequest.setChecksum("calculatedchecksum");
        return addAudioMetadataRequest;
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
    private void makeAddAudioCall() throws Exception {
        makeAddAudioCall(1000L);
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
    private void makeAddAudioCall(long fileSize, String... casesToMapTo) throws Exception {
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
            addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT, "Bristol", "1", fileSize);
        } else {
            addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT, "Bristol", "1", fileSize, casesToMapTo);
        }

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(Path.of(Thread.currentThread().getContextClassLoader()
                                                                 .getResource("sample6.mp2").getFile()))));

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
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
    private Integer uploadAnotherAudioWithSize(long sizeToUse, String expectedAntecedantId, String expectedChronicleId) throws Exception {
        makeAddAudioCall(sizeToUse);

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
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearing.getId());

            assertEquals(1, mediaEntities.size());

            media = mediaEntities.get(0);
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(STARTED_AT, media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            assertEquals(3, media.getCaseNumberList().size());
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
        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllByHearingId(hearingEntity.getId());
        assertEquals(0, mediaEntities.size());//shouldn't have any as no audio in that courtroom
        assertFalse(Objects.requireNonNull(LogUtil.getMemoryLogger())
                        .searchLogs("Uploading new version of duplicate", toLevel(
                            Level.INFO_INT)).isEmpty());

        return media.getId();
    }

    @SuppressWarnings("PMD.LooseCoupling")
    public <T> void streamFileWithMetaData(InMemoryMultipart multipartFile, T metadata, String url) {
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        map.put("metadata", Collections.singletonList(metadata));
        map.put("file", List.of(multipartFile.getResource()));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);

        restTemplate.postForEntity(url, requestEntity, String.class);
    }
}