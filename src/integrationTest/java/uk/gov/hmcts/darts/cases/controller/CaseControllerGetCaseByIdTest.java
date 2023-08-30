package uk.gov.hmcts.darts.cases.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class CaseControllerGetCaseByIdTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;

    private static String endpointUrl = "/cases/{caseId}";

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "1";

    @BeforeEach
    void setUp() {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            SOME_DATE_TIME.toLocalDate()
        );
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.addProsecutor("aProsecutor");
        courtCase.addDefendant("aDefendant");
        courtCase.addDefence("aDefence");
        dartsDatabase.save(courtCase);

        CourthouseEntity courthouseEntity = hearingEntity.getCourtroom().getCourthouse();
        assertEquals(SOME_COURTHOUSE, courthouseEntity.getCourthouseName());

        UserAccountEntity testUser = dartsDatabase.createAuthorisedIntegrationTestUser(courthouseEntity);
        when(mockUserIdentity.getEmailAddress()).thenReturn(testUser.getEmailAddress());
    }

    @Test
    void casesSearchGetEndpoint() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        mockMvc.perform(requestBuilder).andExpect(status().isOk());

        verify(mockUserIdentity).getEmailAddress();

    }

    @Test
    void casesSearchGetEndpointUserIsNotAuthorisedForCourthouse() throws Exception {

        dartsDatabase.createUnauthorisedIntegrationTestUser();

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        MvcResult response = mockMvc.perform(requestBuilder)
            .andExpect(status().isUnauthorized())
            .andReturn();

        String actualResponseBody = response.getResponse().getContentAsString();

        String expectedResponseBody = """
            {
                "type": "AUTHORISATION_100",
                "title": "User is not authorised for the associated courthouse",
                "status": 401
            }
            """;

        JSONAssert.assertEquals(expectedResponseBody, actualResponseBody, JSONCompareMode.NON_EXTENSIBLE);

        verify(mockUserIdentity).getEmailAddress();

    }

    @Test
    void casesSearchGetEndpointCheckListsAreCorrectSize() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(
                "$.case_id",
                Matchers.is(getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE))
            ))
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

        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());

    }

    private Integer getCaseId(String caseNumber, String courthouse) {

        CourtCaseEntity courtCase = dartsDatabase.createCase(courthouse, caseNumber);

        return courtCase.getId();
    }

}
