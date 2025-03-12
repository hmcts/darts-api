package uk.gov.hmcts.darts.dailylist.service.impl.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.task.runner.dailylist.mapper.DailyListRequestMapper;
import uk.gov.hmcts.darts.task.runner.dailylist.mapper.DailyListRequestMapperImpl;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.DailyListStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.XmlParser;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.deserializer.LocalDateTimeTypeDeserializer;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.deserializer.LocalDateTypeDeserializer;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.deserializer.OffsetDateTimeTypeDeserializer;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.serializer.LocalDateTimeTypeSerializer;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.serializer.LocalDateTypeSerializer;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.serializer.OffsetDateTimeTypeSerializer;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

class DailyListRequestMapperTest {

    final XmlParser xmlParser = new XmlParser();

    final DailyListRequestMapper dailyListRequestMapper = new DailyListRequestMapperImpl();


    private ObjectMapper getObjMapper() {
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeTypeSerializer())
            .addSerializer(LocalDate.class, new LocalDateTypeSerializer())
            .addSerializer(OffsetDateTime.class, new OffsetDateTimeTypeSerializer())
            .addDeserializer(LocalDateTime.class, new LocalDateTimeTypeDeserializer())
            .addDeserializer(LocalDate.class, new LocalDateTypeDeserializer())
            .addDeserializer(OffsetDateTime.class, new OffsetDateTimeTypeDeserializer());
        return new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(module);
    }

    @Test
    void mapToEntity_ReturnsDailyListForSnaresbrook() throws Exception {
        String requestXml = TestUtils.getContentsFromFile(
            "Tests/dailylist/DailyListRequestMapperTest/test1/request.xml");
        DailyListStructure legacyDailyList = xmlParser.unmarshal(requestXml, DailyListStructure.class);
        DailyListJsonObject modernisedDailyList = dailyListRequestMapper.mapToEntity(legacyDailyList);

        String actualResponse = getObjMapper().writeValueAsString(modernisedDailyList);
        String expectedResponse = TestUtils.getContentsFromFile(
            "Tests/dailylist/DailyListRequestMapperTest/test1/expectedResponse.json");

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void mapToEntity_ReturnsDailyListForSwansea() throws Exception {
        String requestXml = TestUtils.getContentsFromFile(
            "Tests/dailylist/DailyListRequestMapperTest/test2/request.xml");
        DailyListStructure legacyDailyList = xmlParser.unmarshal(requestXml, DailyListStructure.class);
        DailyListJsonObject modernisedDailyList = dailyListRequestMapper.mapToEntity(legacyDailyList);

        String actualResponse = getObjMapper().writeValueAsString(modernisedDailyList);
        String expectedResponse = TestUtils.getContentsFromFile(
            "Tests/dailylist/DailyListRequestMapperTest/test2/expectedResponse.json");

        TestUtils.compareJson(expectedResponse, actualResponse, List.of());
    }
}
