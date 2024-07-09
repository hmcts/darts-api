package uk.gov.hmcts.darts.cases.controller;

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
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static java.util.stream.IntStream.rangeClosed;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.SECTION_11_1981_DB_ID;

@AutoConfigureMockMvc
class CaseControllerGetEventByCaseIdTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    private static String endpointUrl = "/cases/{case_id}/events";

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "1";

    private HearingEntity hearingEntity;

    private List<EventEntity> eventEntityList;

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
        dartsDatabase.save(courtCase);

        eventEntityList = createEventsWithDefaults(1).stream()
            .map(eve -> dartsDatabase.addHandlerToEvent(eve, SECTION_11_1981_DB_ID))
            .toList();

        dartsDatabase.saveEventsForHearing(hearingEntity, eventEntityList);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void casesGetEventsEndpointShouldReturnForbiddenError() throws Exception {

        when(mockUserIdentity.getUserAccount()).thenReturn(null);

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void casesGetEventsEndpoint() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            [{"id":<event-id>,
            "hearing_id":<hearing-id>,
            "hearing_date":"2023-01-01",
            "timestamp":"2023-01-01T12:00:00Z",
            "name":"Section 11 of the Contempt of Court Act 1981",
            "text":"some-event-text-1",
            }]
            """;

        expectedJson = expectedJson.replace("<event-id>", eventEntityList.get(0).getId().toString());
        expectedJson = expectedJson.replace("<hearing-id>", hearingEntity.getId().toString());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void casesGetEventsEndpointCaseNotFound() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, 25);

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isNotFound());

    }

    private Integer getCaseId(String caseNumber, String courthouse) {

        CourtCaseEntity courtCase = dartsDatabase.createCase(courthouse, caseNumber);

        return courtCase.getId();
    }

    private List<EventEntity> createEventsWithDefaults(int quantity) {
        return rangeClosed(1, quantity)
            .mapToObj(index -> {
                var event = dartsDatabase.getEventStub().createDefaultEvent();
                event.setEventText("some-event-text-" + index);
                event.setTimestamp(SOME_DATE_TIME);
                return event;
            }).toList();
    }

}