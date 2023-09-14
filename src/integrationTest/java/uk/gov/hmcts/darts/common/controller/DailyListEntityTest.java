package uk.gov.hmcts.darts.common.controller;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DailyListEntityTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    @Test
    void dailyListAddDailyListEndpoint() throws Exception {
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457);

        String jsonDocument = getContentsFromFile("tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");
        MockHttpServletRequestBuilder requestBuilder = post("/dailylists")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam("source_system", "CPP")
            .header("json_document", jsonDocument);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        assertThat(response.getResponse().getContentAsString()).contains("dal_id");
    }

    private String getContentsFromFile(String filelocation) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File file = new File(classLoader.getResource(filelocation).getFile());
        return FileUtils.readFileToString(file, "UTF-8");
    }
}
