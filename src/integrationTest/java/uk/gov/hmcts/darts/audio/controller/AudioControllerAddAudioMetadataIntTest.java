package uk.gov.hmcts.darts.audio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@Transactional
class AudioControllerAddAudioMetadataIntTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/audios");
    private static final OffsetDateTime STARTED_AT = OffsetDateTime.now().minusHours(1);
    private static final OffsetDateTime ENDED_AT = OffsetDateTime.now();
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthorisationStub authorisationStub;
    @Autowired
    private DartsDatabaseStub dartsDatabaseStub;
    @MockBean
    private UserIdentity mockUserIdentity;

    @BeforeEach
    void beforeEach() {
        authorisationStub.givenTestSchema();

        UserAccountEntity testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void addAudioMetadata() throws Exception {

        UserAccountEntity testUser = authorisationStub.getSystemUser();
        dartsDatabaseStub.getUserAccountRepository().save(testUser);

        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "Bristol");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp3",
            "audio/mpeg",
            "Test Document (doc)".getBytes()
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes());


        mockMvc.perform(
            multipart(ENDPOINT)
                .file(audioFile)
                .file(metadataJson))
            .andExpect(status().isOk())
            .andReturn();

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository().findAll();

        List<HearingEntity> addAudioLinkedHearings = new ArrayList<>();
        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(3, addAudioLinkedHearings.size());

        for (HearingEntity hearing : addAudioLinkedHearings) {
            MediaEntity media = hearing.getMediaList().get(0);
            assertEquals(1, hearing.getMediaList().size());
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(ENDED_AT, media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            assertEquals(3, media.getCaseIdList().size());
            assertEquals("1", media.getCourtroom().getName());
        }
    }

    @Test
    void addAudioMetadataNonExistingCourthouse() throws Exception {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "TEST");

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "audio.mp3",
            "audio/mpeg",
            "Test Document (doc)".getBytes()
        );

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes());

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

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt, String courthouse) {
        AddAudioMetadataRequest addAudioMetadataRequest = new AddAudioMetadataRequest();
        addAudioMetadataRequest.startedAt(startedAt);
        addAudioMetadataRequest.endedAt(endedAt);
        addAudioMetadataRequest.setChannel(1);
        addAudioMetadataRequest.totalChannels(2);
        addAudioMetadataRequest.format("mp3");
        addAudioMetadataRequest.filename("test");
        addAudioMetadataRequest.courthouse(courthouse);
        addAudioMetadataRequest.courtroom("1");
        addAudioMetadataRequest.cases(List.of("case1", "case2", "case3"));
        addAudioMetadataRequest.setMediaFile("media file");
        addAudioMetadataRequest.setFileSize(1000L);
        addAudioMetadataRequest.setChecksum("calculatedchecksum");
        return addAudioMetadataRequest;
    }
}
