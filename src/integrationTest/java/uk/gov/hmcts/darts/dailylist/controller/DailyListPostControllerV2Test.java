package uk.gov.hmcts.darts.dailylist.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListRequest;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DailyListStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.isA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
class DailyListPostControllerV2Test extends IntegrationBase {

    private static final String ENDPOINT_URL = "/dailylists/v2";

    @Autowired
    protected DailyListStub dailyListStub;
    @Autowired
    private transient MockMvc mockMvc;
    @MockBean
    private UserIdentity mockUserIdentity;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    private static String getExpectedResponse() {
        return "{\"type\":\"DAILYLIST_101\",\"title\":\"Either xml_document or json_document or both needs to be provided.\",\"status\":400}";
    }

    @Test
    void shouldSuccessfullyPostDailyListWhenJsonAndValidSourceSystem() throws Exception {

        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        String courthouseName = "func-swansea-house-" + randomAlphanumeric(7);
        String uniqueId = "func-unique-id-" + randomAlphanumeric(7);
        String messageId = "func-unique-id-" + randomAlphanumeric(7);

        final String jsonPostRequest = getContentsFromFile("tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");

        PostDailyListRequest request = new PostDailyListRequest();
        request.setSourceSystem("CPP");
        request.setCourthouse(courthouseName);
        request.setHearingDate(LocalDate.of(2020, 10, 10));
        request.setUniqueId(uniqueId);
        request.setPublishedTs(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        request.setMessageId(messageId);
        request.setJsonString(jsonPostRequest);

        String requestBody = objectMapper.writeValueAsString(request);
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .content(requestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.dal_id").value(isA(Integer.class)));

    }

    @Test
    void shouldSuccessfullyPostDailyListWhenXmlAndValidQueryParams() throws Exception {

        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        String courthouseName = "func-swansea-house-" + randomAlphanumeric(7);
        String uniqueId = "func-unique-id-" + randomAlphanumeric(7);
        String messageId = "func-unique-id-" + randomAlphanumeric(7);

        PostDailyListRequest request = new PostDailyListRequest();
        request.setSourceSystem("CPP");
        request.setCourthouse(courthouseName);
        request.setHearingDate(LocalDate.of(2020, 10, 10));
        request.setUniqueId(uniqueId);
        request.setPublishedTs(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        request.setMessageId(messageId);
        request.setXmlDocument("<?xml version=\"1.0\"?><dummy></dummy>");

        String requestBody = objectMapper.writeValueAsString(request);
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .content(requestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE);


        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.dal_id").value(isA(Integer.class)));

    }

    @Test
    void shouldFailPostDailyListWhenValidXmlAndEmptySourceSystem() throws Exception {

        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        String courthouseName = "func-swansea-house-" + randomAlphanumeric(7);
        String uniqueId = "func-unique-id-" + randomAlphanumeric(7);
        String messageId = "func-unique-id-" + randomAlphanumeric(7);

        final String expectedResponse = "{\"type\":\"DAILYLIST_105\",\"title\":\"Invalid source system. Should be CPP or XHB.\",\"status\":400}";
        final String jsonPostRequest = getContentsFromFile("tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");


        PostDailyListRequest request = new PostDailyListRequest();
        request.setSourceSystem("");
        request.setCourthouse(courthouseName);
        request.setHearingDate(LocalDate.of(2020, 10, 10));
        request.setUniqueId(uniqueId);
        request.setPublishedTs(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        request.setMessageId(messageId);
        request.setXmlDocument(jsonPostRequest);

        String requestBody = objectMapper.writeValueAsString(request);
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .content(requestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is4xxClientError()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void shouldFailValidationForPostDailyListWhenNoXmlOrJsonPassed() throws Exception {

        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        String courthouseName = "func-swansea-house-" + randomAlphanumeric(7);
        String uniqueId = "func-unique-id-" + randomAlphanumeric(7);
        String messageId = "func-unique-id-" + randomAlphanumeric(7);

        final String expectedResponse = getExpectedResponse();

        PostDailyListRequest request = new PostDailyListRequest();
        request.setSourceSystem("CPP");
        request.setCourthouse(courthouseName);
        request.setHearingDate(LocalDate.of(2020, 10, 10));
        request.setUniqueId(uniqueId);
        request.setPublishedTs(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        request.setMessageId(messageId);

        String requestBody = objectMapper.writeValueAsString(request);
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .content(requestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is4xxClientError()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void shouldFailValidationForPostDailyListWhenValidXmlAndSourceSystemInvalid() throws Exception {

        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        String courthouseName = "func-swansea-house-" + randomAlphanumeric(7);
        String uniqueId = "func-unique-id-" + randomAlphanumeric(7);
        String messageId = "func-unique-id-" + randomAlphanumeric(7);

        final String expectedResponse = "{\"type\":\"DAILYLIST_105\",\"title\":\"Invalid source system. Should be CPP or XHB.\",\"status\":400}";

        final String jsonPostRequest = getContentsFromFile("tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");

        PostDailyListRequest request = new PostDailyListRequest();
        request.setSourceSystem("CPP");
        request.setCourthouse(courthouseName);
        request.setHearingDate(LocalDate.of(2020, 10, 10));
        request.setUniqueId(uniqueId);
        request.setPublishedTs(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        request.setMessageId(messageId);
        request.setXmlDocument(jsonPostRequest);

        String requestBody = objectMapper.writeValueAsString(request);
        requestBody = requestBody.replace("CPP", "TEST");
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .content(requestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is4xxClientError()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void shouldSuccessfullyPostDailyListWhenValidJsonAndNoSourceSystem() throws Exception {

        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        String courthouseName = "func-swansea-house-" + randomAlphanumeric(7);
        String uniqueId = "func-unique-id-" + randomAlphanumeric(7);
        String messageId = "func-unique-id-" + randomAlphanumeric(7);

        final String jsonPostRequest = getContentsFromFile("tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");

        PostDailyListRequest request = new PostDailyListRequest();
        request.setSourceSystem("CPP");
        request.setCourthouse(courthouseName);
        request.setHearingDate(LocalDate.of(2020, 10, 10));
        request.setUniqueId(uniqueId);
        request.setPublishedTs(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        request.setMessageId(messageId);
        request.setJsonString(jsonPostRequest);

        String requestBody = objectMapper.writeValueAsString(request);
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .content(requestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.dal_id").value(isA(Integer.class)));

    }
}

