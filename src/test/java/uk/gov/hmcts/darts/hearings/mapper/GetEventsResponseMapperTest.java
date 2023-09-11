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
import java.util.List;

import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

class GetEventsResponseMapperTest {

    ObjectMapper objectMapper;

    @BeforeEach
    void beforeEach() {
        if (objectMapper == null) {
            ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
            objectMapper = objectMapperConfig.objectMapper();
        }
    }

    @Test
    void testMapToEvents() throws IOException {
        EventHandlerEntity eventType = new EventHandlerEntity();
        eventType.setEventName("TestName");

        HearingEntity hearingEntity = CommonTestDataUtil.createHearing("TEST_1", LocalTime.NOON);
        EventEntity eventEntity = CommonTestDataUtil.createEventWith("LOG", "Test", hearingEntity, eventType);

        List<EventResponse> eventResponses = GetEventsResponseMapper.mapToEvents(List.of(eventEntity));

        String actualResponse = objectMapper.writeValueAsString(eventResponses);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testMapToEvents/expectedResponse.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }
}
