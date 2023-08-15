package uk.gov.hmcts.darts.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.model.CourtLogsPostRequestBody;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.hmcts.darts.testutils.data.CommonTestData.createOffsetDateTime;
import static uk.gov.hmcts.darts.testutils.data.EventTestData.createEventWith;

@AutoConfigureMockMvc
class EventsControllerCourtLogsTest extends IntegrationBase {

    public static final String NEW_CASE = "Case0000001";
    public static final String END_DATE_TIME = "end_date_time";
    public static final String START_DATE_TIME = "start_date_time";
    public static final String CASE_NUMBER = "case_number";
    public static final String COURTHOUSE = "courthouse";

    @Autowired
    private EventRepository eventRepository;

    private static final URI ENDPOINT = URI.create("/courtlogs");
    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";
    private static final String SOME_TEXT = "some-text";
    public static final String LOG = "LOG";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void courtLogsPostShouldPersistLogEventToDatabaseAndReturnCreated() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsBytes(createRequestBody()));

        Assertions.assertEquals(0, getAllLogEventsMatchingText().size(), "Precondition failed");

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isCreated());

        List<EventEntity> persistedEvents = getAllLogEventsMatchingText();

        Assertions.assertEquals(1, persistedEvents.size());
        EventEntity persistedEvent = persistedEvents.get(0);

        EventHandlerEntity eventType = persistedEvent.getEventType();
        Assertions.assertEquals(LOG, eventType.getType());
        Assertions.assertNull(eventType.getSubType());
        Assertions.assertEquals(LOG, eventType.getEventName());

        Assertions.assertNotNull(persistedEvent.getId());
        Assertions.assertEquals(SOME_TEXT, persistedEvent.getEventText());
        Assertions.assertEquals(SOME_DATE_TIME, persistedEvent.getTimestamp());
        Assertions.assertEquals(SOME_COURTROOM, persistedEvent.getCourtroom().getName());
        Assertions.assertEquals(SOME_COURTHOUSE, persistedEvent.getCourtroom().getCourthouse().getCourthouseName());
        Assertions.assertDoesNotThrow(() -> UUID.fromString(persistedEvent.getMessageId()));
        Assertions.assertEquals(1, persistedEvent.getVersion());

        Assertions.assertNull(persistedEvent.getLegacyEventId());
        Assertions.assertNull(persistedEvent.getLegacyVersionLabel());
        Assertions.assertNull(persistedEvent.getSuperseded());
    }

    @Test
    void courtLogsPostShouldReturnBadRequestWhenNoRequestBodyIsProvided() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isBadRequest()).andExpect(
            MockMvcResultMatchers.header().string(
                "Content-Type",
                "application/problem+json"
            ));
    }

    private CourtLogsPostRequestBody createRequestBody() {
        return new CourtLogsPostRequestBody(
            SOME_DATE_TIME,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            Collections.singletonList(SOME_CASE_ID),
            SOME_TEXT
        );
    }

    private List<EventEntity> getAllLogEventsMatchingText() {
        return dartsDatabase.getAllEvents()
            .stream()
            .filter(eventEntity -> LOG.equals(eventEntity.getEventType().getType()))
            .filter(eventEntity -> SOME_TEXT.equals(eventEntity.getEventText()))
            .toList();
    }

    @Test
    void courtLogsGet() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam(COURTHOUSE, "Swansea")
            .queryParam(CASE_NUMBER, NEW_CASE)
            .queryParam(START_DATE_TIME, String.valueOf(createOffsetDateTime("2022-07-01T09:00:00")))
            .queryParam(END_DATE_TIME, String.valueOf(createOffsetDateTime("2022-07-01T11:00:00")))
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk());

    }

    @Test
    void courtLogsGetNoParametersPassed() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT).contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isBadRequest()).andExpect(
            MockMvcResultMatchers.header().string(
                "Content-Type",
                "application/problem+json"
            ));
    }

    @Test
    void courtLogsGetResultMatch() throws Exception {

        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().get(0);

        var event = createEventWith(LOG, "test", hearingEntity, createOffsetDateTime("2023-07-01T10:00:00"));
        eventRepository.saveAndFlush(event);

        String courthouseName = hearingEntity.getCourtCase().getCourthouse().getCourthouseName();
        String caseNumber = hearingEntity.getCourtCase().getCaseNumber();

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam(COURTHOUSE, courthouseName)
            .queryParam(CASE_NUMBER, caseNumber)
            .queryParam(START_DATE_TIME, "2022-07-01T09:00:00+01")
            .queryParam(END_DATE_TIME, "2024-07-01T12:00:00+01")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].courthouse", Matchers.is(courthouseName)))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].caseNumber", Matchers.is(caseNumber)))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].timestamp", Matchers.is(Matchers.notNullValue())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].eventText", Matchers.is("test")));
    }

    @Test
    void courtlogsGetOnlyExpectedResults() throws Exception {

        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().get(0);

        var eventTime = createOffsetDateTime("2023-07-01T10:00:00");
        var event = createEventWith(LOG, "test", hearingEntity, eventTime);
        var event2 = createEventWith(LOG, "Tester", hearingEntity, eventTime);
        var event3 = createEventWith("Event", "ShouldNotShow", hearingEntity, eventTime);
        var event4 = createEventWith("Event", "ShouldAlsoNotShow", hearingEntity, eventTime);

        eventRepository.saveAndFlush(event);
        eventRepository.saveAndFlush(event2);
        eventRepository.saveAndFlush(event3);
        eventRepository.saveAndFlush(event4);

        String courthouseName = hearingEntity.getCourtCase().getCourthouse().getCourthouseName();
        String caseNumber = hearingEntity.getCourtCase().getCaseNumber();

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam(COURTHOUSE, courthouseName)
            .queryParam(CASE_NUMBER, caseNumber)
            .queryParam(START_DATE_TIME, "2022-07-01T09:00:00+01")
            .queryParam(END_DATE_TIME, "2024-07-01T12:00:00+01")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2)));
    }

    @Test
    void courtLogsWrongCaseNumber() throws Exception {

        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().get(0);

        var eventTime = createOffsetDateTime("2023-07-01T10:00:00");
        var event = createEventWith(LOG, "eventText", hearingEntity, eventTime);
        var event2 = createEventWith(LOG, "Tester", hearingEntity, eventTime);

        HearingEntity hearingEntity1 = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            NEW_CASE,
            SOME_COURTHOUSE,
            "CR1",
            SOME_DATE_TIME.toLocalDate()
        );

        var eventHearing = createEventWith(LOG, "eventText", hearingEntity1, eventTime);
        var eventHearing2 = createEventWith(LOG, "eventText2", hearingEntity1, eventTime);

        eventRepository.saveAndFlush(event);
        eventRepository.saveAndFlush(event2);
        eventRepository.saveAndFlush(eventHearing);
        eventRepository.saveAndFlush(eventHearing2);

        String courthouseName = hearingEntity.getCourtCase().getCourthouse().getCourthouseName();

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam(COURTHOUSE, SOME_COURTHOUSE)
            .queryParam(CASE_NUMBER, NEW_CASE)
            .queryParam(START_DATE_TIME, "2022-07-01T09:00:00+01")
            .queryParam(END_DATE_TIME, "2024-07-01T12:00:00+01")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].courthouse", Matchers.is(courthouseName)))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].caseNumber", Matchers.is(NEW_CASE)))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].timestamp", Matchers.is(Matchers.notNullValue())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].eventText", Matchers.notNullValue()))
            .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2)));
    }


}
