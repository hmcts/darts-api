package uk.gov.hmcts.darts.common.controller;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.DailyListPatchRequest;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListResponse;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.CPP;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.XHIBIT;

@AutoConfigureMockMvc
class DailyListEntityTest extends IntegrationBase {

    public static final String DAILYLISTS = "/dailylists";
    public static final String SOURCE_SYSTEM = "source_system";
    public static final String JSON_STRING = "json_string";
    public static final String DAL_ID = "dal_id";
    @Autowired
    private transient MockMvc mockMvc;

    @MockBean
    UserIdentity mockUserIdentity;

    @Test
    void dailyListAddDailyListEndpoint() throws Exception {
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");

        String jsonDocument = getContentsFromFile("tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");
        MockHttpServletRequestBuilder requestBuilder = post(DAILYLISTS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam(SOURCE_SYSTEM, "CPP")
            .header(JSON_STRING, jsonDocument);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        assertThat(response.getResponse().getContentAsString()).contains(DAL_ID);
    }

    @Test
    void dailyListAddEmptyDailyListEndpoint() throws Exception {
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");

        String jsonDocument = "";
        MockHttpServletRequestBuilder requestBuilder = post(DAILYLISTS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam(SOURCE_SYSTEM, "CPP")
            .header(JSON_STRING, jsonDocument);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is5xxServerError()).andReturn();

        assertEquals(500, response.getResponse().getStatus());
    }

    @Test
    void dailyListAddXmlDailyListEndpoint() throws Exception {
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");

        String xmlString = "<?xml version=\"1.0\"?><dummy></dummy>";
        MockHttpServletRequestBuilder requestBuilder = post(DAILYLISTS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam(SOURCE_SYSTEM, "CPP")
            .header("xml_document", xmlString);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is4xxClientError()).andReturn();

        assertEquals(400, response.getResponse().getStatus());
    }

    private String getContentsFromFile(String filelocation) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File file = new File(classLoader.getResource(filelocation).getFile());
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    @Test
    void dailyListPatchDailyListEndpoint() throws Exception {
        when(mockUserIdentity.userHasGlobalAccess(Set.of(XHIBIT, CPP))).thenReturn(true);

        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");

        String jsonDocument1 = getContentsFromFile("tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");
        String jsonDocument2 = getContentsFromFile("tests/dailylist/DailyListServiceTest/insert1_ok/DailyListRequest.json");

        MockHttpServletRequestBuilder requestBuilder = post(DAILYLISTS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam(SOURCE_SYSTEM, "CPP")
            .header(JSON_STRING, jsonDocument1);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        DailyListJsonObject dailyList = objectMapper.readValue(jsonDocument1, DailyListJsonObject.class);
        PostDailyListResponse postDailyListResponse = objectMapper.readValue(response.getResponse().getContentAsString(), PostDailyListResponse.class);

        DailyListPatchRequest patchRequest = new DailyListPatchRequest();
        patchRequest.setDailyListId(postDailyListResponse.getDalId());
        patchRequest.setDailyListJson(dailyList);

        String dalID = String.valueOf(postDailyListResponse.getDalId());

        MockHttpServletRequestBuilder requestBuilder2 = patch(DAILYLISTS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam(SOURCE_SYSTEM, "CPP")
            .queryParam(DAL_ID, dalID)
            .header(JSON_STRING, jsonDocument2);
        MvcResult patchResponse = mockMvc.perform(requestBuilder2).andExpect(status().isOk()).andReturn();

        assertThat(patchResponse.getResponse().getContentAsString()).contains(DAL_ID);
    }

}
