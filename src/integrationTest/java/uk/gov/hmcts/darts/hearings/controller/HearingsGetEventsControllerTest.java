package uk.gov.hmcts.darts.hearings.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;
import static uk.gov.hmcts.darts.test.common.data.DefenceTestData.createDefenceForCase;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCase;
import static uk.gov.hmcts.darts.test.common.data.ProsecutorTestData.createProsecutorForCase;

@AutoConfigureMockMvc
@Slf4j
class HearingsGetEventsControllerTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    private static final String ENDPOINT_URL = "/hearings/{hearingId}/events";

    @MockitoBean
    private UserIdentity mockUserIdentity;

    private HearingEntity hearingEntity;
    private EventEntity event;

    private static final String SOME_DATE = "2023-01-01";

    @BeforeEach
    void setUp() {

        var courtCase = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();
        courtCase.addProsecutor(createProsecutorForCase(courtCase));
        courtCase.addDefendant(createDefendantForCase(courtCase));
        courtCase.addDefence(createDefenceForCase(courtCase));
        var hearing = PersistableFactory.getHearingTestData().createHearingWith(courtCase, someMinimalCourtRoom(), LocalDate.parse(SOME_DATE));

        dartsPersistence.save(hearing);

        event = dartsDatabase.createEvent(hearing);

        hearingEntity = dartsDatabase.getHearingRepository().findById(hearing.getId()).orElseThrow();
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void okGet() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearingEntity.getId());
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        log.info("actualJson: {}", actualJson);
        String expectedJson = """
            [{
              "id":<<eventId>>,
              "timestamp":"2020-06-20T10:00:00Z",
              "name":"Defendant recalled",
              "text":"testEventText",
              "is_data_anonymised": false
            }]
            """;
        expectedJson = expectedJson.replace("<<eventId>>", event.getId().toString());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void errorGetNotFound() throws Exception {
        int hearingId = -1;

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearingId);
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

    @Test
    void hearingEventsGetEndpointShouldReturnForbiddenError() throws Exception {

        when(mockUserIdentity.getUserAccount()).thenReturn(null);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearingEntity.getId());
        MvcResult response = mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"AUTHORISATION_106","title":"Could not obtain user details","status":403}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }
}