package uk.gov.hmcts.darts.cases.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Disabled("Impacted by V1_357__add_missing_constraints_part5b.sql")
@AutoConfigureMockMvc
@Disabled("Impacted by V1_363__not_null_constraints_part3.sql")
class CaseControllerGetCaseByIdTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    private static String endpointUrl = "/cases/{case_id}";

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "1";

    private HearingEntity hearingEntity;

    @MockBean
    private UserIdentity mockUserIdentity;

    @BeforeEach
    void setUp() {
        hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.addProsecutor("aProsecutor");
        courtCase.addDefendant("aDefendant");
        courtCase.addDefence("aDefence");
        dartsDatabase.save(courtCase);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void casesSearchGetEndpointShouldReturnForbiddenError() throws Exception {

        when(mockUserIdentity.getUserAccount()).thenReturn(null);

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void casesSearchGetEndpoint() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"case_id":<case-id>,
            "courthouse_id":<courthouse-id>,
            "courthouse":"some-courthouse",
            "case_number":"1",
            "defendants":["aDefendant"],
            "judges":["1JUDGE1"],
            "prosecutors":["aProsecutor"],
            "defenders":["aDefence"],
            "reporting_restrictions":[]
            }
            """;

        expectedJson = expectedJson.replace("<case-id>", hearingEntity.getCourtCase().getId().toString());
        expectedJson = expectedJson.replace("<courthouse-id>", hearingEntity.getCourtCase().getCourthouse().getId().toString());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void casesSearchGetEndpointCheckListsAreCorrectSize() throws Exception {

        final Integer caseId = getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE);
        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, caseId);

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.case_id", Matchers.is(caseId)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.judges", Matchers.hasSize(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.judges[0]", Matchers.is("1JUDGE1")))
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

    private Integer getCaseId(String caseNumber, String courthouse) {

        CourtCaseEntity courtCase = dartsDatabase.createCase(courthouse, caseNumber);

        return courtCase.getId();
    }

}
