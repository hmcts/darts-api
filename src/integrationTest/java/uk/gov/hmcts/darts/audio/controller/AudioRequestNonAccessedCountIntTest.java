package uk.gov.hmcts.darts.audio.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@Slf4j
@Transactional
class AudioRequestNonAccessedCountIntTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/audio-requests/not-accessed-count");
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getNonAccessedAudioCountForUser() throws Exception {
        var requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        dartsDatabase.createAndLoadNonAccessedCurrentMediaRequestEntity(requestor);
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .header("user_id", requestor.getId().toString());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").isNumber())
            .andExpect(jsonPath("$.count").value(1))
            .andReturn();

    }

    @Test
    void getNonAccessedAudioCountForUser_Zero() throws Exception {
        var requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .header("user_id", requestor.getId().toString());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").isNumber())
            .andExpect(jsonPath("$.count").value(0))
            .andReturn();

    }

    @Test
    void getNonAccessedAudioCountForUser_NonExistingUser() throws Exception {
        var requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .header("user_id", requestor.getId().toString());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").isNumber())
            .andExpect(jsonPath("$.count").value(0))
            .andReturn();
    }
}
