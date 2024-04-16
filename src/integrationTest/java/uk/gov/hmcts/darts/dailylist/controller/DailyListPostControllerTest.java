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
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DailyListStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.LocalDate;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.isA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
class DailyListPostControllerTest extends IntegrationBase {

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

        String todayDateString = LocalDate.now().toString();
        String tomorrowDateString = LocalDate.now().plusDays(1).toString();

        final String jsonPostRequest = getContentsFromFile("tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");

        MockHttpServletRequestBuilder requestBuilder = post("/dailylists")
            .queryParam("source_system", "CPP")
            .queryParam("courthouse", courthouseName)
            .queryParam("hearing_date", tomorrowDateString)
            .queryParam("unique_id", uniqueId)
            .queryParam("published_ts", todayDateString + "T23:30:52.123Z")
            .queryParam("message_id", messageId)
            .header("json_string", jsonPostRequest)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.dal_id").value(isA(Integer.class)));

    }

    @Test
    void shouldSuccessfullyPostDailyListWhenXmlAndValidQueryParams() throws Exception {

        String xmlString = "<?xml version=\"1.0\"?><dummy></dummy>";

        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        String courthouseName = "func-swansea-house-" + randomAlphanumeric(7);
        String uniqueId = "func-unique-id-" + randomAlphanumeric(7);
        String messageId = "func-unique-id-" + randomAlphanumeric(7);

        String todayDateString = LocalDate.now().toString();
        String tomorrowDateString = LocalDate.now().plusDays(1).toString();

        MockHttpServletRequestBuilder requestBuilder = post("/dailylists")
            .queryParam("source_system", "CPP")
            .queryParam("courthouse", courthouseName)
            .queryParam("hearing_date", tomorrowDateString)
            .queryParam("unique_id", uniqueId)
            .queryParam("published_ts", todayDateString + "T23:30:52.123Z")
            .queryParam("message_id", messageId)
            .header("xml_document", xmlString)
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

        String todayDateString = LocalDate.now().toString();
        String tomorrowDateString = LocalDate.now().plusDays(1).toString();

        MockHttpServletRequestBuilder requestBuilder = post("/dailylists")
            .queryParam("source_system", "")
            .queryParam("courthouse", courthouseName)
            .queryParam("hearing_date", tomorrowDateString)
            .queryParam("unique_id", uniqueId)
            .queryParam("published_ts", todayDateString + "T23:30:52.123Z")
            .queryParam("message_id", messageId)
            .header("xml_document", jsonPostRequest)
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

        String todayDateString = LocalDate.now().toString();
        String tomorrowDateString = LocalDate.now().plusDays(1).toString();

        MockHttpServletRequestBuilder requestBuilder = post("/dailylists")
            .queryParam("source_system", "CPP")
            .queryParam("courthouse", courthouseName)
            .queryParam("hearing_date", tomorrowDateString)
            .queryParam("unique_id", uniqueId)
            .queryParam("published_ts", todayDateString + "T23:30:52.123Z")
            .queryParam("message_id", messageId)
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

        String todayDateString = LocalDate.now().toString();
        String tomorrowDateString = LocalDate.now().plusDays(1).toString();

        final String jsonPostRequest = getContentsFromFile("tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");


        MockHttpServletRequestBuilder requestBuilder = post("/dailylists")
            .queryParam("source_system", "RUB")
            .queryParam("courthouse", courthouseName)
            .queryParam("hearing_date", tomorrowDateString)
            .queryParam("unique_id", uniqueId)
            .queryParam("published_ts", todayDateString + "T23:30:52.123Z")
            .queryParam("message_id", messageId)
            .header("xml_document", jsonPostRequest)
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

        String todayDateString = LocalDate.now().toString();
        String tomorrowDateString = LocalDate.now().plusDays(1).toString();

        final String jsonPostRequest = getContentsFromFile("tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");

        MockHttpServletRequestBuilder requestBuilder = post("/dailylists")
            .queryParam("courthouse", courthouseName)
            .queryParam("hearing_date", tomorrowDateString)
            .queryParam("unique_id", uniqueId)
            .queryParam("published_ts", todayDateString + "T23:30:52.123Z")
            .queryParam("message_id", messageId)
            .header("json_string", jsonPostRequest)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.dal_id").value(isA(Integer.class)));

    }
    }

