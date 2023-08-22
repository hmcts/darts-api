package uk.gov.hmcts.darts.audio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
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
class AudioControllerAddAudioRequestIntTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/audio/request");
    private static final AudioRequestType AUDIO_REQUEST_TYPE = AudioRequestType.PLAYBACK;
    private static final OffsetDateTime START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime END_TIME = OffsetDateTime.parse("2023-01-01T13:00:00Z");
    private static final int REQUESTOR = 1;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addAudioRequestPostShouldReturnSuccess() throws Exception {
        assertEquals(0, dartsDatabase.getMediaRequestRepository()
            .findAll()
            .size(), "Precondition failed");

        var hearingEntity = dartsDatabase.hasSomeHearing();
        var audioRequestDetails = createAudioRequestDetails(hearingEntity);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(audioRequestDetails));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isCreated());

        List<MediaRequestEntity> mediaRequestEntities = dartsDatabase.getMediaRequestRepository()
            .findAll();
        assertEquals(1, mediaRequestEntities.size());

        var mediaRequestEntity = mediaRequestEntities.get(0);
        assertEquals(hearingEntity.getId(), mediaRequestEntity.getHearing().getId());
        assertEquals(REQUESTOR, mediaRequestEntity.getRequestor().getId());
        assertEquals(START_TIME, mediaRequestEntity.getStartTime());
        assertEquals(END_TIME, mediaRequestEntity.getEndTime());
        assertEquals(AUDIO_REQUEST_TYPE, mediaRequestEntity.getRequestType());
        assertEquals(AudioRequestStatus.OPEN, mediaRequestEntity.getStatus());
        assertEquals(0, mediaRequestEntity.getAttempts());
    }

    private AudioRequestDetails createAudioRequestDetails(HearingEntity hearingEntity) {
        var audioRequestDetails = new AudioRequestDetails();

        audioRequestDetails.setRequestType(AUDIO_REQUEST_TYPE);
        audioRequestDetails.setHearingId(hearingEntity.getId());
        audioRequestDetails.setStartTime(START_TIME);
        audioRequestDetails.setEndTime(END_TIME);
        audioRequestDetails.setRequestor(REQUESTOR);

        return audioRequestDetails;
    }

}
