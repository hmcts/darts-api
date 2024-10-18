package uk.gov.hmcts.darts.hearings.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.hearings.model.EventResponse;

import java.io.IOException;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

class GetEventsResponseMapperTest {

    ObjectMapper objectMapper;

    EventHandlerEntity eventType = new EventHandlerEntity();

    @BeforeEach
    void beforeEach() {
        if (objectMapper == null) {
            ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
            objectMapper = objectMapperConfig.objectMapper();
        }
        eventType.setEventName("TestName");
    }

    @Test
    void testMapToEvents() throws IOException {
        HearingEntity hearingEntity = CommonTestDataUtil.createHearing("TEST_1", LocalTime.NOON);
        EventEntity eventEntity = CommonTestDataUtil.createEventWith("Test", hearingEntity, eventType);

        List<EventResponse> eventResponses = GetEventsResponseMapper.mapToEvents(List.of(eventEntity));

        String actualResponse = objectMapper.writeValueAsString(eventResponses);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testMapToEvents/expectedResponse.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void testMapToEventsEventAnonymised() throws IOException {
        HearingEntity hearingEntity = CommonTestDataUtil.createHearing("TEST_1", LocalTime.NOON);
        EventEntity eventEntity = CommonTestDataUtil.createEventWith("Test", hearingEntity, eventType);
        eventEntity.setDataAnonymised(true);
        List<EventResponse> eventResponses = GetEventsResponseMapper.mapToEvents(List.of(eventEntity));

        String actualResponse = objectMapper.writeValueAsString(eventResponses);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testMapToEvents/expectedResponseAnonymised.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void testVersionedEventsFilteredOut() throws IOException {
        OffsetDateTime eventDate = OffsetDateTime.parse("2024-07-01T12:00Z");

        HearingEntity hearingEntity = CommonTestDataUtil.createHearing("TEST_1", LocalTime.NOON);
        EventEntity eventEntity1 = CommonTestDataUtil.createEventWith(1, 1, "Event1", hearingEntity, eventType, eventDate, eventDate, true);
        EventEntity eventEntity2 = CommonTestDataUtil.createEventWith(2, 1, "Event2", hearingEntity, eventType, eventDate, eventDate.minusHours(1), true);
        EventEntity eventEntity3 = CommonTestDataUtil.createEventWith(3, 1, "Event3", hearingEntity, eventType, eventDate, eventDate.plusHours(2), true);
        EventEntity eventEntity4 = CommonTestDataUtil.createEventWith(4, 1, "Event4", hearingEntity, eventType, eventDate, eventDate.plusHours(1), true);

        List<EventResponse> eventResponses = GetEventsResponseMapper.mapToEvents(List.of(eventEntity1, eventEntity2, eventEntity3, eventEntity4));

        String actualResponse = objectMapper.writeValueAsString(eventResponses);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testVersionedEvents/expectedResponseForVersionedEvents.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void testVersionedEventsWithIdZeroAreNotFilteredOut() throws IOException {
        OffsetDateTime eventDate = OffsetDateTime.parse("2024-07-01T12:00Z");

        HearingEntity hearingEntity = CommonTestDataUtil.createHearing("TEST_1", LocalTime.NOON);
        EventEntity eventEntity1 = CommonTestDataUtil.createEventWith(1, 0, "Event1", hearingEntity, eventType, eventDate, eventDate, true);
        EventEntity eventEntity2 = CommonTestDataUtil.createEventWith(2, 0, "Event2", hearingEntity, eventType, eventDate, eventDate.plusHours(1), false);

        List<EventResponse> eventResponses = GetEventsResponseMapper.mapToEvents(List.of(eventEntity1, eventEntity2));

        String actualResponse = objectMapper.writeValueAsString(eventResponses);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testVersionedEvents/expectedResponseForZeroIdVersionedEvents.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void testVersionedEventsWithGroupedEventsIsCurrentSetToTrueAndFalse() throws IOException {
        OffsetDateTime eventDate = OffsetDateTime.parse("2024-07-01T12:00Z");

        HearingEntity hearingEntity = CommonTestDataUtil.createHearing("TEST_1", LocalTime.NOON);
        EventEntity eventEntity1 = CommonTestDataUtil.createEventWith(1, 1, "Event1", hearingEntity, eventType, eventDate, eventDate, false);
        EventEntity eventEntity2 = CommonTestDataUtil.createEventWith(2, 1, "Event2", hearingEntity, eventType, eventDate, eventDate.plusHours(1), true);
        EventEntity eventEntity3 = CommonTestDataUtil.createEventWith(3, 1, "Event3", hearingEntity, eventType, eventDate, eventDate.plusHours(2), false);
        EventEntity eventEntity4 = CommonTestDataUtil.createEventWith(4, 2, "Event4", hearingEntity, eventType, eventDate, eventDate, true);
        EventEntity eventEntity5 = CommonTestDataUtil.createEventWith(5, 2, "Event5", hearingEntity, eventType, eventDate, eventDate.plusHours(1), true);

        List<EventResponse> eventResponses = GetEventsResponseMapper.mapToEvents(List.of(eventEntity1, eventEntity2, eventEntity3, eventEntity4, eventEntity5));

        String actualResponse = objectMapper.writeValueAsString(eventResponses);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testVersionedEvents/expectedResponseForVersionedEventsWithGroupedEventsIsCurrentSet.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void testVersionedEventsWithNullEventId() throws IOException {
        OffsetDateTime eventDate = OffsetDateTime.parse("2024-07-01T12:00Z");

        HearingEntity hearingEntity = CommonTestDataUtil.createHearing("TEST_1", LocalTime.NOON);
        EventEntity eventEntity1 = CommonTestDataUtil.createEventWith(1, null, "Event1", hearingEntity, eventType, eventDate, eventDate, true);
        EventEntity eventEntity2 = CommonTestDataUtil.createEventWith(2, null, "Event2", hearingEntity, eventType, eventDate, eventDate.plusHours(1), false);

        List<EventResponse> eventResponses = GetEventsResponseMapper.mapToEvents(List.of(eventEntity1, eventEntity2));

        String actualResponse = objectMapper.writeValueAsString(eventResponses);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testVersionedEvents/expectedResponseForZeroIdVersionedEvents.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }
}
