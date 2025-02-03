package uk.gov.hmcts.darts.common.controller;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
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
    public static final String DAL_ID = "dal_id";
    @Autowired
    private transient MockMvc mockMvc;

    @MockitoBean
    UserIdentity mockUserIdentity;

    @Test
    void dailyListAddDailyListEndpoint() throws Exception {
        when(mockUserIdentity.userHasGlobalAccess(Set.of(XHIBIT, CPP))).thenReturn(true);
        when(mockUserIdentity.getUserAccount()).thenReturn(dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity());

        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");

        String requestBody = getContentsFromFile("tests/dailylist/DailyListEntityTest/dailyListAddDailyListEndpoint/requestBody.json");
        MockHttpServletRequestBuilder requestBuilder = post(DAILYLISTS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        assertThat(response.getResponse().getContentAsString()).contains(DAL_ID);
    }

    @Test
    void dailyListAddEmptyDailyListEndpoint() throws Exception {
        when(mockUserIdentity.userHasGlobalAccess(Set.of(XHIBIT, CPP))).thenReturn(true);

        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");

        String requestBody = """
            {
              "source_system": "CPP",
              "json_string": "{"
            }""";
        MockHttpServletRequestBuilder requestBuilder = post(DAILYLISTS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is5xxServerError()).andReturn();

        assertEquals(500, response.getResponse().getStatus());
    }

    @Test
    void dailyListAddXmlDailyListEndpointMissingCriteria() throws Exception {
        when(mockUserIdentity.userHasGlobalAccess(Set.of(XHIBIT, CPP))).thenReturn(true);
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");

        String requestBody = """
            {
              "source_system": "CPP",
              "message_id": "some-message-id-example",
              "xml_document": "<dummy></dummy>"
            }""";
        MockHttpServletRequestBuilder requestBuilder = post(DAILYLISTS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is4xxClientError()).andReturn();

        assertEquals(400, response.getResponse().getStatus());
    }

    @Test
    void dailyListAddDailyListEndpointNoAuth() throws Exception {
        when(mockUserIdentity.userHasGlobalAccess(Set.of(XHIBIT, CPP))).thenReturn(false);
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");

        String requestBody = """
            {
              "source_system": "CPP",
              "message_id": "some-message-id-example",
              "xml_document": "<dummy></dummy>"
            }""";
        MockHttpServletRequestBuilder requestBuilder = post(DAILYLISTS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is4xxClientError()).andReturn();

        assertEquals(403, response.getResponse().getStatus());
    }

    private String getContentsFromFile(String filelocation) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File file = new File(classLoader.getResource(filelocation).getFile());
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    @Test
    void dailyListPatchDailyListEndpoint() throws Exception {
        when(mockUserIdentity.userHasGlobalAccess(Set.of(XHIBIT, CPP))).thenReturn(true);
        when(mockUserIdentity.getUserAccount()).thenReturn(dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity());

        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");

        String postRequestBody = getContentsFromFile("tests/dailylist/DailyListEntityTest/dailyListPatchDailyListEndpoint/postRequestBody.json");

        MockHttpServletRequestBuilder requestBuilder = post(DAILYLISTS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(postRequestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        PostDailyListResponse postDailyListResponse = objectMapper.readValue(response.getResponse().getContentAsString(), PostDailyListResponse.class);

        String dalID = String.valueOf(postDailyListResponse.getDalId());

        String patchRequestBody = getContentsFromFile("tests/dailylist/DailyListEntityTest/dailyListPatchDailyListEndpoint/patchRequestBody.json");
        patchRequestBody = patchRequestBody.replace("<<dal_id>>", dalID);
        MockHttpServletRequestBuilder requestBuilder2 = patch(DAILYLISTS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam(SOURCE_SYSTEM, "CPP")
            .content(patchRequestBody);
        MvcResult patchResponse = mockMvc.perform(requestBuilder2).andExpect(status().isOk()).andReturn();

        assertThat(patchResponse.getResponse().getContentAsString()).contains(DAL_ID);
    }

}
