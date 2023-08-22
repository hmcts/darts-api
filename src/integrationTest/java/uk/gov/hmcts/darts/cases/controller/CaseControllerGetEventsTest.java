package uk.gov.hmcts.darts.cases.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.EventHandlerTestData;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseAt;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.testutils.data.EventTestData.createEventWith;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createHearingWithDefaults;
import static uk.gov.hmcts.darts.testutils.data.JudgeTestData.createJudgeWithName;

@AutoConfigureMockMvc
class CaseControllerGetEventsTest  extends IntegrationBase {

    public static final String EVENT_TEXT1 = "event1";
    @Autowired
    private transient MockMvc mockMvc;

    private static String endpointUrl = "/cases/hearings/{hearingId}/events";

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";

    private HearingEntity hearing1;
    private EventEntity event1;
    private EventHandlerEntity activeHandler;

    @BeforeEach
    void setupData() {
        CourthouseEntity swanseaCourthouse = someMinimalCourthouse();
        swanseaCourthouse.setCourthouseName(SOME_COURTHOUSE);

        CourtroomEntity courtroom1 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, SOME_COURTROOM);

        CourtCaseEntity case1 = createCaseAt(swanseaCourthouse);
        case1.setCaseNumber("Case1");

        JudgeEntity judge = createJudgeWithName("aJudge");
        hearing1 = createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 20), judge);

        dartsDatabase.saveAll(hearing1);

        activeHandler = EventHandlerTestData.someMinimalEventHandler();

        dartsDatabase.saveAll(activeHandler);

        event1 = createEventWith("eventName", EVENT_TEXT1, hearing1, OffsetDateTime.now(), activeHandler);
        EventEntity event2 = createEventWith("eventName", "event2", hearing1, OffsetDateTime.now(), activeHandler);
        dartsDatabase.saveAll(event1, event2);
    }

    @AfterEach
    void tearDown() {
        dartsDatabase.addToTrash(activeHandler);
    }


    @Test
    void hearingGetEventsWithInvalidHearingIdReturnsNotFoundEndpoint() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, 456);
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void hearingsGetEvents() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, hearing1.getId());

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.*", Matchers.hasSize(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id", Matchers.is(event1.getId())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].timestamp", Matchers.is(Matchers.notNullValue())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].name", Matchers.is("some-desc")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].text", Matchers.is(EVENT_TEXT1)));

    }

}
