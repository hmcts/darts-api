package uk.gov.hmcts.darts.common.controller;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.dailylist.controller.DailyListController;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest()
@ContextConfiguration(classes = DailyListController.class)
@ActiveProfiles("intTest")
class DailyListTest {

    @Autowired
    private transient MockMvc mockMvc;

    @Test
    void dailyListAddDailyListEndpoint() throws Exception {
        String requestBody = getContentsFromFile("tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");
        MockHttpServletRequestBuilder requestBuilder = post("/dailylist/addDailyList")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam("source_system", "CPP")
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotImplemented()).andReturn();

        assertThat(response.getResponse().getContentAsString()).isEqualTo("");
    }

    @Test
    void dailyListGetCasesEndpoint() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/dailylist/getCases")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam("court_house_code", "457")
            .queryParam("court_room_number", "1")
            .queryParam("hearing_date", "2023-02-02");
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotImplemented()).andReturn();

        assertThat(response.getResponse().getContentAsString()).isEqualTo("");
    }

    private String getContentsFromFile(String filelocation) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File file = new File(classLoader.getResource(filelocation).getFile());
        return FileUtils.readFileToString(file, "UTF-8");
    }
}
