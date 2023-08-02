package uk.gov.hmcts.darts.cases.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@AutoConfigureMockMvc
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class CaseControllerGetCaseHearingsTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    private static String endpointUrl = "/cases/{caseId}/hearings";
    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";

    @BeforeEach
    void setUp() {

        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            SOME_DATE_TIME.toLocalDate()
        );

    }

    @Test
    void casesSearchGetEndpoint() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, "25");

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk());

    }

    @Test
    void casesSearchGetEndpointOneObjectReturned() throws Exception {

        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().get(0);

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, hearingEntity.getCourtCase().getId());

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id", Matchers.is(hearingEntity.getId())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].date", Matchers.is(Matchers.notNullValue())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].judges", Matchers.is(Matchers.notNullValue())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].courtroom", Matchers.is(SOME_COURTROOM)));

    }

    @Test
    void casesSearchGetEndpointMultipleObjectsReturned() throws Exception {

        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            "CR1",
            SOME_DATE_TIME.toLocalDate()
        );

        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().get(0);
        HearingEntity hearingEntity2 = dartsDatabase.getHearingRepository().findAll().get(1);

        hearingEntity.addJudge(dartsDatabase.createSimpleJudge("hearing1Judge"));
        hearingEntity2.addJudge(dartsDatabase.createSimpleJudge("hearing2Judge"));

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, hearingEntity.getCourtCase().getId());

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id", Matchers.is(hearingEntity.getId())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].date", Matchers.is(Matchers.notNullValue())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].judges", Matchers.is(Matchers.notNullValue())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].courtroom", Matchers.is(SOME_COURTROOM)))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].id", Matchers.is(hearingEntity2.getId())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].courtroom", Matchers.is("CR1")));

    }

    @Test
    void casesSearchGetEndpointNoObjectsReturned() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, "25");

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(MockMvcResultMatchers.jsonPath(
            "$[0]").doesNotExist());

    }

}
