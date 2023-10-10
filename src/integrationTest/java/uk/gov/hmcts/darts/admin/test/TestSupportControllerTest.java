package uk.gov.hmcts.darts.admin.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TestSupportControllerTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    private static final String ENDPOINT_URL = "/functional-tests";

    @Test
    void rejectsCourthousesNotPrefixedCorrectly() throws Exception {
        var requestBuilder = post(ENDPOINT_URL + "/courthouse/swansea/courtroom/cr1");

        mockMvc.perform(requestBuilder).andExpect(status().isBadRequest());
    }

    @Test
    void createsCourthouseAndCourtroom() throws Exception {
        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/func-swansea/courtroom/cr1"))
            .andExpect(status().isCreated());

        assertThat(dartsDatabase.findCourtroomBy("func-swansea", "cr1")).isNotNull();
    }

    @Test
    void createsCourtroomForExistingCourthouse() throws Exception {
        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/func-swansea/courtroom/cr1"))
            .andExpect(status().isCreated());

        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/func-swansea/courtroom/cr2"))
            .andExpect(status().isCreated());

        assertThat(dartsDatabase.findCourtroomBy("func-swansea", "cr1")).isNotNull();
        assertThat(dartsDatabase.findCourtroomBy("func-swansea", "cr2")).isNotNull();
    }

    @Test
    void cleansData() throws Exception {
        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/func-swansea/courtroom/func-cr1"))
            .andExpect(status().isCreated());

        mockMvc.perform(delete(ENDPOINT_URL + "/clean"))
            .andExpect(status().is2xxSuccessful());

        assertThat(dartsDatabase.findCourtroomBy("func-swansea", "cr1")).isNull();
    }

}
