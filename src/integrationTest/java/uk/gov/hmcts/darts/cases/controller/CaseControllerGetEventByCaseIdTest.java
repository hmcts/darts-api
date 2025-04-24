package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import uk.gov.hmcts.darts.cases.model.Event;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.PaginationTestSupport;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.util.pagination.PaginatedList;

import java.time.OffsetDateTime;
import java.util.List;

import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.SECTION_11_1981_DB_ID;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.SECTION_39_1933_DB_ID;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.SECTION_4_1981_DB_ID;

@AutoConfigureMockMvc
class CaseControllerGetEventByCaseIdTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    private static final String ENDPOINT_URL = "/cases/{case_id}/events";

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "1";
    private static final String SOME_CASE_NUMBER_TWO = "2";

    private HearingEntity hearingEntity;

    private List<EventEntity> eventEntityList;

    @MockitoBean
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

        HearingEntity hearingEntity2 = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER_TWO,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );
        courtCase = hearingEntity2.getCourtCase();
        dartsDatabase.save(courtCase);

        dartsDatabase.saveEventsForHearing(hearingEntity2, eventEntityList);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void casesGetEventsEndpointShouldReturnForbiddenError() throws Exception {

        when(mockUserIdentity.getUserAccount()).thenReturn(null);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void casesGetEventsEndpoint() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            [{"id":<event-id>,
            "hearing_id":<hearing-id>,
            "hearing_date":"2023-01-01",
            "timestamp":"2023-01-01T12:01:00Z",
            "name":"Section 11 of the Contempt of Court Act 1981",
            "text":"some-event-text-2023-01-01T12:01",
            "is_data_anonymised": false,
            "courtroom":"TESTCOURTROOM"
            }]
            """;

        expectedJson = expectedJson.replace("<event-id>", eventEntityList.getFirst().getId().toString());
        expectedJson = expectedJson.replace("<hearing-id>", hearingEntity.getId().toString());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

        // assert that we only ever got one event. The one that was associated to the first case hearing.
        // Relates to verification of https://tools.hmcts.net/jira/browse/DMP-3967
        Assertions.assertEquals(1, eventEntityList.size());
    }


    @Test
    void casesGetEventsEndpoint_shouldBeSortedByTimestamp() throws Exception {

        HearingEntity hearingEntity2 = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );
        CourtCaseEntity courtCase = hearingEntity2.getCourtCase();
        dartsDatabase.save(courtCase);

        eventEntityList = createEventsWithDefaults(1).stream()
            .map(eve -> dartsDatabase.addHandlerToEvent(eve, SECTION_11_1981_DB_ID))
            .toList();
        EventEntity eventEntity = eventEntityList.getFirst();
        eventEntity.setEventId(3);
        eventEntity.setTimestamp(eventEntity.getTimestamp().plusDays(1));

        dartsDatabase.saveEventsForHearing(hearingEntity2, eventEntityList);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            
             [
              {
                "id": 2,
                "hearing_id": 1,
                "hearing_date": "2023-01-01",
                "timestamp": "2023-01-02T12:01:00Z",
                "name": "Section 11 of the Contempt of Court Act 1981",
                "is_data_anonymised": false,
                "text": "some-event-text-2023-01-01T12:01",
                "courtroom":"TESTCOURTROOM"
              },
              {
                "id": 1,
                "hearing_id": 1,
                "hearing_date": "2023-01-01",
                "timestamp": "2023-01-01T12:01:00Z",
                "name": "Section 11 of the Contempt of Court Act 1981",
                "is_data_anonymised": false,
                "text": "some-event-text-2023-01-01T12:01",
                "courtroom":"TESTCOURTROOM"
              }
            ]
            """;

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    void casesGetEventsEndpointCaseNotFound() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, 25);

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isNotFound());

    }

    @Nested
    @DisplayName("Paginated casesGetEvents")
    class CasesGetEventsPaginated {
        private static final String SOME_CASE_NUMBER = "12321";
        @Autowired
        private PaginationTestSupport paginationTestSupport;

        private List<EventEntity> eventEntityList1;
        private List<EventEntity> eventEntityList2;
        private List<EventEntity> eventEntityList3;
        private List<EventEntity> eventEntityList4;
        private List<EventEntity> eventEntityList5;

        @BeforeEach
        void beforeEach() {
            CaseControllerGetEventByCaseIdTest.super.clearTestData();
            OffsetDateTime baseTime = OffsetDateTime.parse("2020-01-01T12:00Z");
            eventEntityList1 = createHearingWithEvents(baseTime, 2, SECTION_4_1981_DB_ID); //1,2
            eventEntityList2 = createHearingWithEvents(baseTime.minusDays(1), 2, SECTION_4_1981_DB_ID);//4,5
            eventEntityList3 = createHearingWithEvents(baseTime.plusHours(1), 2, SECTION_11_1981_DB_ID);//6,7
            eventEntityList4 = createHearingWithEvents(baseTime.minusDays(2), 2, SECTION_11_1981_DB_ID);//8,9
            eventEntityList5 = createHearingWithEvents(baseTime.plusHours(2), 2, SECTION_39_1933_DB_ID);//10,11
        }

        private List<EventEntity> createHearingWithEvents(OffsetDateTime date, int numberOfEvents, int handlerId) {
            HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
                SOME_CASE_NUMBER,
                SOME_COURTHOUSE,
                SOME_COURTROOM,
                date.toLocalDateTime()
            );
            CourtCaseEntity courtCase = hearingEntity.getCourtCase();
            dartsDatabase.save(courtCase);

            List<EventEntity> eventEntityList = createEventsWithDefaults(numberOfEvents, date).stream()
                .map(eve -> dartsDatabase.addHandlerToEvent(eve, handlerId))
                .toList();

            dartsDatabase.saveEventsForHearing(hearingEntity, eventEntityList);
            return eventEntityList;
        }

        @Test
        void casesGetEvents_usingPaginatedCrieria_WithNoOrder_shouldReturnPaginatedResultsUsingDefaultOrder_10Resultslimit3Page1() throws Exception {
            MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE))
                .queryParam("page_number", "1")
                .queryParam("page_size", "3");

            MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            PaginatedList<Event> paginatedList = paginationTestSupport.getPaginatedList(mvcResult, Event.class);
            paginationTestSupport.assertPaginationDetails(paginatedList, 1, 3, 4, 10);
            assertThat(
                paginatedList.getData()
                    .stream().map(Event::getId)
                    .toList())
                .contains(
                    eventEntityList5.get(1).getId(),
                    eventEntityList5.get(0).getId(),
                    eventEntityList3.get(1).getId());

            JSONAssert.assertEquals(
                """
                    {
                      "current_page": 1,
                      "total_pages": 4,
                      "page_size": 3,
                      "total_items": 10,
                      "data": [
                        {
                          "id": 11,
                          "hearing_id": 3,
                          "hearing_date": "2020-01-01",
                          "timestamp": "2020-01-01T14:02:00Z",
                          "name": "Section 39 of the Children and Young Persons Act 1933",
                          "is_data_anonymised": false,
                          "text": "some-event-text-2020-01-01T14:02",
                          "courtroom": "TESTCOURTROOM"
                        },
                        {
                          "id": 10,
                          "hearing_id": 3,
                          "hearing_date": "2020-01-01",
                          "timestamp": "2020-01-01T14:01:00Z",
                          "name": "Section 39 of the Children and Young Persons Act 1933",
                          "is_data_anonymised": false,
                          "text": "some-event-text-2020-01-01T14:01",
                          "courtroom": "TESTCOURTROOM"
                        },
                        {
                          "id": 7,
                          "hearing_id": 3,
                          "hearing_date": "2020-01-01",
                          "timestamp": "2020-01-01T13:02:00Z",
                          "name": "Section 11 of the Contempt of Court Act 1981",
                          "is_data_anonymised": false,
                          "text": "some-event-text-2020-01-01T13:02",
                          "courtroom": "TESTCOURTROOM"
                        }
                      ]
                    }
                    """,
                TestUtils.writeAsString(paginatedList),
                JSONCompareMode.STRICT);
        }

        @Test
        void casesGetEvents_usingPaginatedCrieria_WithCustomOrderHearingDateAsc_shouldReturnPaginatedResultsUsingCustomOrder_10Resultslimit3Page1()
            throws Exception {
            MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE))
                .queryParam("page_number", "1")
                .queryParam("page_size", "3")
                .queryParam("sort_order", "ASC")
                .queryParam("sort_by", "hearingDate");

            MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            PaginatedList<Event> paginatedList = paginationTestSupport.getPaginatedList(mvcResult, Event.class);
            paginationTestSupport.assertPaginationDetails(paginatedList, 1, 3, 4, 10);
            assertThat(
                paginatedList.getData()
                    .stream().map(Event::getId)
                    .toList())
                .contains(
                    eventEntityList4.get(0).getId(),
                    eventEntityList4.get(1).getId(),
                    eventEntityList2.get(1).getId());
        }

        @Test
        void casesGetEvents_usingPaginatedCrieria_WithCustomOrderHearingDateDesc_shouldReturnPaginatedResultsUsingCustomOrder_10Resultslimit3Page1()
            throws Exception {
            MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE))
                .queryParam("page_number", "1")
                .queryParam("page_size", "3")
                .queryParam("sort_order", "DESC")
                .queryParam("sort_by", "hearingDate");

            MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            PaginatedList<Event> paginatedList = paginationTestSupport.getPaginatedList(mvcResult, Event.class);
            paginationTestSupport.assertPaginationDetails(paginatedList, 1, 3, 4, 10);
            assertThat(
                paginatedList.getData()
                    .stream().map(Event::getId)
                    .toList())
                .contains(
                    eventEntityList5.get(1).getId(),
                    eventEntityList5.get(0).getId(),
                    eventEntityList3.get(1).getId());
        }


        @Test
        void casesGetEvents_usingPaginatedCrieria_WithCustomOrderTimestamp_shouldReturnPaginatedResultsUsingCustomOrder_10Resultslimit3Page1()
            throws Exception {
            MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE))
                .queryParam("page_number", "1")
                .queryParam("page_size", "3")
                .queryParam("sort_order", "DESC")
                .queryParam("sort_by", "timestamp");

            MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            PaginatedList<Event> paginatedList = paginationTestSupport.getPaginatedList(mvcResult, Event.class);
            paginationTestSupport.assertPaginationDetails(paginatedList, 1, 3, 4, 10);
            assertThat(
                paginatedList.getData()
                    .stream().map(Event::getId)
                    .toList())
                .contains(
                    eventEntityList5.get(1).getId(),
                    eventEntityList5.get(0).getId(),
                    eventEntityList3.get(1).getId());
        }

        @Test
        void casesGetEvents_usingPaginatedCrieria_WithCustomOrderEventName_shouldReturnPaginatedResultsUsingCustomOrder_10Resultslimit3Page1()
            throws Exception {
            MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE))
                .queryParam("page_number", "1")
                .queryParam("page_size", "3")
                .queryParam("sort_order", "ASC,ASC")
                .queryParam("sort_by", "event,time");

            MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

            PaginatedList<Event> paginatedList = paginationTestSupport.getPaginatedList(mvcResult, Event.class);
            paginationTestSupport.assertPaginationDetails(paginatedList, 1, 3, 4, 10);
            assertThat(
                paginatedList.getData()
                    .stream().map(Event::getId)
                    .toList())
                .contains(
                    eventEntityList4.get(0).getId(),
                    eventEntityList4.get(1).getId(),
                    eventEntityList3.get(0).getId());
        }
    }

    private Integer getCaseId(String caseNumber, String courthouse) {

        CourtCaseEntity courtCase = dartsDatabase.createCase(courthouse, caseNumber);

        return courtCase.getId();
    }

    private List<EventEntity> createEventsWithDefaults(int quantity) {
        return createEventsWithDefaults(quantity, SOME_DATE_TIME);
    }

    private List<EventEntity> createEventsWithDefaults(int quantity, OffsetDateTime timestampBase) {
        return rangeClosed(1, quantity)
            .mapToObj(index -> {
                var timestamp = timestampBase.plusMinutes(index);
                var event = dartsDatabase.getEventStub().createDefaultEvent();
                event.setEventText("some-event-text-" + timestamp.toLocalDateTime());
                event.setTimestamp(timestamp);
                return event;
            }).toList();
    }

}