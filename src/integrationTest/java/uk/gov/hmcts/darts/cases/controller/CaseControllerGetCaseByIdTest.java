package uk.gov.hmcts.darts.cases.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@AutoConfigureMockMvc
class CaseControllerGetCaseByIdTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    private static String endpointUrl = "/cases/{caseId}";

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "1";

    @BeforeEach
    void setUp() {

        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            SOME_DATE_TIME.toLocalDate()
        );

    }

    @Test
    void casesSearchGetEndpoint() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId());

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk());

    }

    @Test
    void casesSearchGetEndpointCheckListsAreCorrectSize() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId());

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.case_id", Matchers.is(getCaseId())))
            .andExpect(MockMvcResultMatchers.jsonPath("$.judges", Matchers.hasSize(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.judges[0]", Matchers.is("1judge1")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.prosecutors", Matchers.hasSize(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.prosecutors[0]", Matchers.is("aProsecutor")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.defendants", Matchers.hasSize(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.defendants[0]", Matchers.is("aDefendant")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.defenders", Matchers.hasSize(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.defenders[0]", Matchers.is("aDefence")));

    }

    @Test
    void casesSearchGetEndpointCaseNotFound() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, 25);

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isNotFound());

    }

    private Integer getCaseId() {

        CourtCaseEntity courtCase = dartsDatabase.createCaseUnlessExists(SOME_CASE_NUMBER, SOME_COURTHOUSE);

        return courtCase.getId();
    }

}
