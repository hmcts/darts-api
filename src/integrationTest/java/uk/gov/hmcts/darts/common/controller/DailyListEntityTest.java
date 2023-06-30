package uk.gov.hmcts.darts.common.controller;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.util.ReprovisionDatabaseBeforeEach;
import uk.gov.hmcts.darts.dailylist.controller.DailyListController;
import uk.gov.hmcts.darts.dailylist.service.DailyListService;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@ReprovisionDatabaseBeforeEach
class DailyListEntityTest {

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private DailyListService dailyListService;

    @Autowired
    private DailyListController dailyListController;


    @Test
    void dailyListAddDailyListEndpoint() throws Exception {
        String requestBody = getContentsFromFile("tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");
        MockHttpServletRequestBuilder requestBuilder = post("/dailylists")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam("source_system", "CPP")
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        assertThat(response.getResponse().getContentAsString()).isEqualTo("");
    }

    @Test
    void dailyListGetCasesEndpoint() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/dailylists/getCases")
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
