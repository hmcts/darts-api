package uk.gov.hmcts.darts.event.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.event.model.CourtLog;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventEntityToCourtLogMapperTest {

    ObjectMapper objectMapper;

    @BeforeAll
    void beforeAll() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @Test
    void testSingleEntityToCourtLog() throws Exception {

        var hearingEntity = CommonTestDataUtil.createHearing("Case0000001", LocalTime.of(10, 0));
        List<EventEntity> event = List.of(CommonTestDataUtil.createEvent("LOG", "Test", hearingEntity));

        List<CourtLog> courtLogs = EventEntityToCourtLogMapper.mapFromEntityToCourtLogs(
            event,
            hearingEntity.getCourtroom().getCourthouse().getCourthouseName(),
            hearingEntity.getCourtCase().getCaseNumber()
        );

        String actualResponse = objectMapper.writeValueAsString(courtLogs);
        String expectedResponse = getContentsFromFile(
            "Tests/CourtLogs/EventEntityToMapperTest/expectedResponseSingleEntity.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void testMultipleEntitiesToCourtLog() throws Exception {

        var hearingEntity = CommonTestDataUtil.createHearing("Case0000001", LocalTime.of(10, 0));
        var hearingEntity2 = CommonTestDataUtil.createHearing("Case0000002", LocalTime.of(10, 0));

        List<EventEntity> entities = new ArrayList<>();

        EventEntity event = CommonTestDataUtil.createEvent("LOG", "Test", hearingEntity);

        EventEntity event2 = CommonTestDataUtil.createEvent("LOG", "Test", hearingEntity2);

        entities.add(event);
        entities.add(event2);

        List<CourtLog> courtLogs = EventEntityToCourtLogMapper.mapFromEntityToCourtLogs(
            entities,
            hearingEntity.getCourtroom().getCourthouse().getCourthouseName(),
            hearingEntity.getCourtCase().getCaseNumber()
        );

        String actualResponse = objectMapper.writeValueAsString(courtLogs);

        String expectedResponse = getContentsFromFile(
            "Tests/CourtLogs/EventEntityToMapperTest/expectedResponseMultipleEntities.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

}
