package uk.gov.hmcts.darts.audio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audiorecording.model.AddAudioRequest;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
class AudioControllerAddAudioIntTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/audios");
    private static final OffsetDateTime STARTED_AT = OffsetDateTime.now().minusHours(1);
    private static final OffsetDateTime ENDED_AT = OffsetDateTime.now();
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    void addAudio() throws Exception {
        dartsDatabase.createCase("SWANSEA", "case1");
        dartsDatabase.createCase("SWANSEA", "case2");
        dartsDatabase.createCase("SWANSEA", "case3");

        AddAudioRequest addAudioRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "SWANSEA");
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(addAudioRequest));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository().findAll();

        assertEquals(3, allHearings.size());

        for (HearingEntity hearing : allHearings) {
            MediaEntity media = hearing.getMediaList().get(0);
            assertEquals(1, hearing.getMediaList().size());
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(ENDED_AT, media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            assertEquals(3, media.getCaseIdList().size());
        }
    }

    @Test
    void addAudioNonExistingCourthouse() throws Exception {
        AddAudioRequest addAudioRequest = createAddAudioRequest(STARTED_AT, ENDED_AT, "TEST");
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(addAudioRequest));

        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"type":"COMMON_100","title":"Provided courthouse does not exist","status":400,"detail":"Courthouse 'TEST' not found."}""";

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    private AddAudioRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt, String courthouse) {
        AddAudioRequest addAudioRequest = new AddAudioRequest();
        addAudioRequest.startedAt(startedAt);
        addAudioRequest.endedAt(endedAt);
        addAudioRequest.setChannel(1);
        addAudioRequest.totalChannels(2);
        addAudioRequest.format("mp3");
        addAudioRequest.filename("test");
        addAudioRequest.courthouse(courthouse);
        addAudioRequest.courtroom("1");
        addAudioRequest.cases(List.of("case1", "case2", "case3"));
        return addAudioRequest;
    }
}
