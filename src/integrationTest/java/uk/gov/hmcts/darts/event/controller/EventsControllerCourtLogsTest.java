package uk.gov.hmcts.darts.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.event.model.CourtLogsPostRequestBody;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class EventsControllerCourtLogsTest extends IntegrationBase {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private CourtroomRepository courtroomRepository;

    @Autowired
    private CourthouseRepository courthouseRepository;

    @Autowired
    private HearingRepository hearingRepository;

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

        assertEquals(0, getAllLogEventsMatchingText().size(), "Precondition failed");

        mockMvc.perform(requestBuilder)
            .andExpect(status().isCreated());

        List<EventEntity> persistedEvents = getAllLogEventsMatchingText();

        assertEquals(1, persistedEvents.size());
        EventEntity persistedEvent = persistedEvents.get(0);

        EventHandlerEntity eventType = persistedEvent.getEventType();
        assertEquals(LOG, eventType.getType());
        assertNull(eventType.getSubType());
        assertEquals(LOG, eventType.getEventName());

        assertNotNull(persistedEvent.getId());
        assertEquals(SOME_TEXT, persistedEvent.getEventText());
        assertEquals(SOME_DATE_TIME, persistedEvent.getTimestamp());
        assertEquals(SOME_COURTROOM, persistedEvent.getCourtroom().getName());
        assertEquals(SOME_COURTHOUSE, persistedEvent.getCourtroom().getCourthouse().getCourthouseName());
        assertDoesNotThrow(() -> UUID.fromString(persistedEvent.getMessageId()));
        assertEquals(0, persistedEvent.getVersion());

        assertNull(persistedEvent.getLegacyEventId());
        assertNull(persistedEvent.getLegacyVersionLabel());
        assertNull(persistedEvent.getSuperseded());
    }

    @Test
    void courtLogsPostShouldReturnBadRequestWhenNoRequestBodyIsProvided() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT).header("Content-Type", "application/json");

        mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andExpect(header().string(
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
            .queryParam("Courthouse", "Swansea")
            .queryParam("caseNumber", "Case0000001")
            .queryParam("startDateTime", String.valueOf(CommonTestDataUtil.createOffsetDateTime("2022-07-01T09:00:00")))
            .queryParam("endDateTime", String.valueOf(CommonTestDataUtil.createOffsetDateTime("2022-07-01T11:00:00")))
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isOk());

    }

    @Test
    void courtLogsGetNoParametersPassed() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT).contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andExpect(header().string(
            "Content-Type",
            "application/problem+json"
        ));
    }

    @Test
    @Transactional
    void courtLogsGetResultMatch() throws Exception {

        var hearingEntity = CommonTestDataUtil.createHearing("Case0000001", LocalTime.of(10, 0));
        hearingRepository.saveAndFlush(hearingEntity);

        var event = CommonTestDataUtil.createEvent("LOG", "test", hearingEntity);
        eventRepository.saveAndFlush(event);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam("Courthouse", "NEWCASTLE")
            .queryParam("caseNumber", "Case0000001")
            .queryParam("startDateTime", "2022-07-01T09:00:00+01")
            .queryParam("endDateTime", "2024-07-01T12:00:00+01")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        System.out.println(mockMvc.perform(requestBuilder).andReturn().getResponse().getContentAsString());

       mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courthouse", is("NEWCASTLE")))
            .andExpect(jsonPath("$[0].caseNumber", is("Case0000001")))
            .andExpect(jsonPath("$[0].timestamp", is(notNullValue())))
            .andExpect(jsonPath("$[0].eventText", is("test")));
    }

}
