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

        MockHttpServletRequestBuilder requestBuilder = post("/dailylists")
            .queryParam("source_system", "CPP")
            .queryParam("courthouse", courthouseName)
            .queryParam("hearing_date", tomorrowDateString)
            .queryParam("unique_id", uniqueId)
            .queryParam("published_ts", todayDateString + "T23:30:52.123Z")
            .queryParam("message_id", messageId)
            .header("json_string", dailyListPostJson())
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

        String todayDateString = LocalDate.now().toString();
        String tomorrowDateString = LocalDate.now().plusDays(1).toString();

        MockHttpServletRequestBuilder requestBuilder = post("/dailylists")
            .queryParam("source_system", "")
            .queryParam("courthouse", courthouseName)
            .queryParam("hearing_date", tomorrowDateString)
            .queryParam("unique_id", uniqueId)
            .queryParam("published_ts", todayDateString + "T23:30:52.123Z")
            .queryParam("message_id", messageId)
            .header("xml_document", dailyListPostJson())
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

        MockHttpServletRequestBuilder requestBuilder = post("/dailylists")
            .queryParam("source_system", "RUB")
            .queryParam("courthouse", courthouseName)
            .queryParam("hearing_date", tomorrowDateString)
            .queryParam("unique_id", uniqueId)
            .queryParam("published_ts", todayDateString + "T23:30:52.123Z")
            .queryParam("message_id", messageId)
            .header("xml_document", dailyListPostJson())
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

        MockHttpServletRequestBuilder requestBuilder = post("/dailylists")
            .queryParam("courthouse", courthouseName)
            .queryParam("hearing_date", tomorrowDateString)
            .queryParam("unique_id", uniqueId)
            .queryParam("published_ts", todayDateString + "T23:30:52.123Z")
            .queryParam("message_id", messageId)
            .header("json_string", dailyListPostJson())
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.dal_id").value(isA(Integer.class)));

    }

    private String dailyListPostJson() {
        return """
            {
              "document_id": {
                "document_name": "DailyList_457_20240319174952.xml",
                "unique_id": "Test12-DMP-2322",
                "document_type": "DL",
                "time_stamp": "2024-04-03T00:00:00.00+01:00"
              },
              "list_header": {
                "list_category": "Criminal",
                "start_date": "2024-04-03",
                "end_date": "2024-04-03",
                "version": "NOT VERSIONED",
                "published_time": "2024-04-03T11:34:00.000Z"
              },
              "crown_court": {
                "court_house_type": "Crown Court",
                "court_house_code": {
                  "court_house_short_name": "DMP-2322_Courthouse",
                  "code": 2322
                },
                "court_house_name": "DMP-2322_Courthouse"
              },
              "court_lists": [
                {
                  "court_house": {
                    "court_house_type": "Crown Court",
                    "court_house_code": {
                      "court_house_short_name": "DMP-2322_Courthouse",
                      "code": 2322
                    },
                    "court_house_name": "DMP-2322_Courthouse"
                  },
                  "sittings": [
                    {
                      "court_room_number": 1,
                      "sitting_sequence_no": 1,
                      "sitting_at": "11:00:00",
                      "sitting_priority": "T",
                      "judiciary": [
                        {
                          "citizen_name_forename": "Judge2322",
                          "citizen_name_surname": "Surname",
                          "citizen_name_requested_name": "Judgename Surname"
                        }
                      ],
                      "hearings": [
                        {
                          "hearing_sequence_number": 1,
                          "hearing_details": {
                            "hearing_type": "PTR",
                            "hearing_description": "For Pre-Trial Review",
                            "hearing_date": "2024-04-03"
                          },
                          "time_marking_note": "NOT BEFORE 11:00 AM",
                          "case_number": "case12_DMP2322",
                          "prosecution": {
                            "prosecuting_authority": "Crown Prosecution Service",
                            "prosecuting_reference": "Crown Prosecution Service",
                            "prosecuting_organisation": {
                              "organisation_name": "Crown Prosecution Service"
                            },
                            "advocates": [
                              {
                                "name": {
                                  "citizen_name_forename": "ProsecutorName",
                                  "citizen_name_surname": "Surname",
                                  "citizen_name_requested_name": ""
                                }
                              }
                            ]
                          },
                          "defendants": [
                            {
                              "personal_details": {
                                "name": {
                                  "citizen_name_forename": "DefendantName",
                                  "citizen_name_surname": "Surname",
                                  "citizen_name_requested_name": ""
                                },
                                "is_masked": "false"
                              },
                              "urn": "Test10_urn_DMP2322",
                              "charges": [
                                {
                                  "indictment_count_number": 1,
                                  "cjsoffence_code": "CA03014",
                                  "offence_statement": "Fail / refuse give assistance to person executing Communications Act search warrant"
                                }
                              ],
                              "counsel": [
                                {
                                  "name": {
                                    "citizen_name_forename": "DefenceName",
                                    "citizen_name_surname": "Surname",
                                    "citizen_name_requested_name": ""
                                  }
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """;
    }
}

