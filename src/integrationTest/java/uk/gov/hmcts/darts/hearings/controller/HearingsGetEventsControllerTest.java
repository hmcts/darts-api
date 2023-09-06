package uk.gov.hmcts.darts.hearings.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@AutoConfigureMockMvc
class HearingsGetEventsControllerTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    private static String endpointUrl = "/hearings/{hearingId}/events";

    @Test
    void okGet() throws Exception {
        HearingEntity hearing = dartsDatabase.createHearing(
            "testCourthouse",
            "testCourtroom",
            "testCaseNumber",
            LocalDate.of(2020, 6, 20)
        );
        JudgeEntity testJudge = dartsDatabase.createSimpleJudge("testJudge");
        hearing.addJudge(testJudge);
        dartsDatabase.save(hearing);

        EventEntity event = dartsDatabase.createEvent(hearing);


        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, hearing.getId());
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();


        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            [{"id":<<eventId>>,"timestamp":"2020-06-20T10:00:00Z","name":"Defendant recalled","text":"testEventText"}]
            """;
        expectedJson = expectedJson.replace("<<eventId>>", event.getId().toString());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void errorGetNotFound() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, -1);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();


        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {
              "type": "HEARING_100",
              "title": "The requested hearing cannot be found",
              "status": 404
            }
            """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

    }

}
