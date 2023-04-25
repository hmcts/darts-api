package uk.gov.hmcts.darts.common.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class GetWelcomeTest {

    @Autowired
    private transient MockMvc mockMvc;

    @DisplayName("Should welcome upon root request with 200 response code")
    @Test
    void welcomeRootEndpoint() throws Exception {
        MvcResult response = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();

        assertThat(response.getResponse().getContentAsString()).startsWith("Welcome");
    }

    @Test
    void dailyListAddDailyListEndpoint() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/dailylist/addDailyList")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam("source_system", "CPP")
            .content("{\n" +
                         "  \"document_id\": {\n" +
                         "    \"document_name\": \"DailyList_457_20210219174938.xml\",\n" +
                         "    \"unique_id\": \"CSDDL1613756980160\",\n" +
                         "    \"document_type\": \"DL\",\n" +
                         "    \"time_stamp\": \"2021-02-19T17:49:38.391Z\"\n" +
                         "  },\n" +
                         "  \"list_header\": {\n" +
                         "    \"list_category\": \"Criminal\",\n" +
                         "    \"start_date\": \"2021-02-23\",\n" +
                         "    \"end_date\": \"2021-02-23\",\n" +
                         "    \"version\": \"NOT VERSIONED\",\n" +
                         "    \"published_time\": \"2021-02-19T17:49:38.767Z\"\n" +
                         "  },\n" +
                         "  \"crown_court\": {\n" +
                         "    \"court_house_type\": \"Crown Court\",\n" +
                         "    \"court_house_code\": {\n" +
                         "      \"court_house_short_name\": \"SWANS\",\n" +
                         "      \"code\": 457\n" +
                         "    },\n" +
                         "    \"court_house_name\": \"SWANSEA\"\n" +
                         "  },\n" +
                         "  \"court_lists\": [\n" +
                         "    {\n" +
                         "      \"court_house\": {\n" +
                         "        \"court_house_type\": \"Crown Court\",\n" +
                         "        \"court_house_code\": {\n" +
                         "          \"court_house_short_name\": \"SWANS\",\n" +
                         "          \"code\": 457\n" +
                         "        },\n" +
                         "        \"court_house_name\": \"SWANSEA\"\n" +
                         "      },\n" +
                         "      \"sittings\": [\n" +
                         "        {\n" +
                         "          \"court_room_number\": 1,\n" +
                         "          \"sitting_sequence_no\": 1,\n" +
                         "          \"sitting_at\": \"11:00:00\",\n" +
                         "          \"sitting_priority\": \"T\",\n" +
                         "          \"judiciary\": [\n" +
                         "            {\n" +
                         "              \"citizen_name_forename\": \"Susan Katherine\",\n" +
                         "              \"citizen_name_surname\": \"Bonnell\",\n" +
                         "              \"citizen_name_requested_name\": \"Mrs Susan Katherine Bonnell JP\"\n" +
                         "            }\n" +
                         "          ],\n" +
                         "          \"hearings\": [\n" +
                         "            {\n" +
                         "              \"hearing_sequence_number\": 1,\n" +
                         "              \"hearing_details\": {\n" +
                         "                \"hearing_type\": \"PTR\",\n" +
                         "                \"hearing_description\": \"For Pre-Trial Review\",\n" +
                         "                \"hearing_date\": \"2021-02-23\"\n" +
                         "              },\n" +
                         "              \"time_marking_note\": \"NOT BEFORE 11:00 AM\",\n" +
                         "              \"case_number\": \"CPP\",\n" +
                         "              \"prosecution\": {\n" +
                         "                \"prosecuting_authority\": \"Crown Prosecution Service\",\n" +
                         "                \"prosecuting_reference\": \"Crown Prosecution Service\",\n" +
                         "                \"prosecuting_organisation\": {\n" +
                         "                  \"organisation_name\": \"Crown Prosecution Service\"\n" +
                         "                }\n" +
                         "              },\n" +
                         "              \"defendants\": [\n" +
                         "                {\n" +
                         "                  \"personal_details\": {\n" +
                         "                    \"name\": {\n" +
                         "                      \"citizen_name_forename\": \"Susan Katherine\",\n" +
                         "                      \"citizen_name_surname\": \"Bonnell\",\n" +
                         "                      \"citizen_name_requested_name\": \"Mrs Susan Katherine Bonnell JP\"\n" +
                         "                    },\n" +
                         "                    \"is_masked\": \"no\"\n" +
                         "                  },\n" +
                         "                  \"urn\": \"42GD2391421\",\n" +
                         "                  \"charges\": [\n" +
                         "                    {\n" +
                         "                      \"indictment_count_number\": 1,\n" +
                         "                      \"cjsoffence_code\": \"CA03014\",\n" +
                         "                      \"offence_statement\": \"Fail    / refuse give assistance to person executing Communications Act search warrant\"\n" +
                         "                    }\n" +
                         "                  ]\n" +
                         "                }\n" +
                         "              ]\n" +
                         "            }\n" +
                         "          ]\n" +
                         "        }\n" +
                         "      ]\n" +
                         "    }\n" +
                         "  ]\n" +
                         "}\n");
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotImplemented()).andReturn();

        assertThat(response.getResponse().getContentAsString()).isEqualTo("");
    }
}
