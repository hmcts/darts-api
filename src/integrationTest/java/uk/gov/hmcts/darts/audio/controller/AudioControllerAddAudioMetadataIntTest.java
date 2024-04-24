package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.unit.DataSize;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.Problem;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AudioControllerAddAudioMetadataIntTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/audios");
    private static final OffsetDateTime STARTED_AT = OffsetDateTime.of(2024, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime ENDED_AT = OffsetDateTime.of(2024, 10, 10, 11, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime ENDED_AT_NEXT_DAY = OffsetDateTime.of(2024, 10, 11, 11, 0, 0, 0, ZoneOffset.UTC);

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

    @Value("${spring.servlet.multipart.max-file-size}")
    private DataSize fileSizeThreshold;

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
        HearingEntity hearingAfter = hearingStub.createHearing("Bristol", "1", "case3", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingAfter, 10, ENDED_AT.plusMinutes(20), "LOG");
    }

    @Test
    void addAudioMetadata() throws Exception {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(new FileInputStream(AudioControllerAddAudioMetadataIntTest.class.getClassLoader()
                    .getResource("sample6.mp2").getFile()))
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
            assertEquals(ENDED_AT, media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            assertEquals(3, media.getCaseNumberList().size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(media.getCourtroom().getId()).get().getName());
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
    void addAudioMetadataNonExistingCourthouse() throws Exception {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "TEST", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(new FileInputStream(AudioControllerAddAudioMetadataIntTest.class.getClassLoader()
                                                        .getResource("sample6.mp2").getFile()))
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
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "Bristol", "1", unknownType);

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(new FileInputStream(AudioControllerAddAudioMetadataIntTest.class.getClassLoader()
                                                        .getResource("sample6.mp2").getFile()))
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
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "Bristol", "1");

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
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT_NEXT_DAY, "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(new FileInputStream(AudioControllerAddAudioMetadataIntTest.class.getClassLoader()
                                                        .getResource("sample6.mp2").getFile()))
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
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "Bristol", "1");

        // create an audio file that throws an exception
        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(new FileInputStream(AudioControllerAddAudioMetadataIntTest.class.getClassLoader()
                                                        .getResource("sample6.mp2").getFile()))
        ) {
            private int FILE_SIGNATURE_CALL_COUNT = 0;
            @Override
            public InputStream getInputStream() throws IOException {
                // fail on any call other than for the file signature validation
                if (FILE_SIGNATURE_CALL_COUNT != 0) {
                    throw new IOException();
                }
                FILE_SIGNATURE_CALL_COUNT = FILE_SIGNATURE_CALL_COUNT + 1;
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
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT_NEXT_DAY, "Bristol", "1");

        // set the file size to be greater than the maximum threshold
        addAudioMetadataRequest.setFileSize(fileSizeThreshold.toBytes() + 1);

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(new FileInputStream(AudioControllerAddAudioMetadataIntTest.class.getClassLoader()
                                                        .getResource("sample6.mp2").getFile()))
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
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.incorrect",
            "audio/mpeg",
            IOUtils.toByteArray(new FileInputStream(AudioControllerAddAudioMetadataIntTest.class.getClassLoader()
                                                        .getResource("sample6.mp2").getFile()))
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
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "Bristol", "1");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp2",
            "audio/mpegincorrect",
            IOUtils.toByteArray(new FileInputStream(AudioControllerAddAudioMetadataIntTest.class.getClassLoader()
                                                        .getResource("sample6.mp2").getFile()))
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
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "Bristol", "1");

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

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt, String courthouse, String courtroom) {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom, "mp2");
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt,
                                                          String courthouse, String courtroom, String filetype) {
        AddAudioMetadataRequest addAudioMetadataRequest = new AddAudioMetadataRequest();
        addAudioMetadataRequest.startedAt(startedAt);
        addAudioMetadataRequest.endedAt(endedAt);
        addAudioMetadataRequest.setChannel(1);
        addAudioMetadataRequest.totalChannels(2);
        addAudioMetadataRequest.format(filetype);
        addAudioMetadataRequest.filename("test");
        addAudioMetadataRequest.courthouse(courthouse);
        addAudioMetadataRequest.courtroom(courtroom);
        addAudioMetadataRequest.cases(List.of("case1", "case2", "case3"));
        addAudioMetadataRequest.setMediaFile("media file");
        addAudioMetadataRequest.setFileSize(1000L);
        addAudioMetadataRequest.setChecksum("calculatedchecksum");
        return addAudioMetadataRequest;
    }
}