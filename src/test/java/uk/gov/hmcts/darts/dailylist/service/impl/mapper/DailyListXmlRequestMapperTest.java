package uk.gov.hmcts.darts.dailylist.service.impl.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListRequest;
import uk.gov.hmcts.darts.task.runner.dailylist.mapper.DailyListXmlRequestMapper;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.DailyListStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.XmlParser;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DailyListXmlRequestMapperTest {

    private final XmlParser xmlParser = new XmlParser();

    @Test
    void testPublishedTimeBstWithTimeZone() throws Exception {
        String requestXml = TestUtils.getContentsFromFile(
            "Tests/dailylist/DailyListXmlRequestMapperTest/publishedDateBstWithTimeZone.xml");
        DailyListStructure dailyList = xmlParser.unmarshal(requestXml, DailyListStructure.class);
        PostDailyListRequest postDailyListRequest = DailyListXmlRequestMapper.mapToPostDailyListRequest(dailyList, requestXml, "XHB", "12345");

        OffsetDateTime publishedTime = postDailyListRequest.getPublishedTs();
        assertEquals("2024-04-23T16:00+01:00", publishedTime.toString());
    }

    @Test
    void testPublishedTimeBstWithoutTimeZone() throws Exception {
        String requestXml = TestUtils.getContentsFromFile(
            "Tests/dailylist/DailyListXmlRequestMapperTest/publishedDateBstWithoutTimeZone.xml");
        DailyListStructure dailyList = xmlParser.unmarshal(requestXml, DailyListStructure.class);
        PostDailyListRequest postDailyListRequest = DailyListXmlRequestMapper.mapToPostDailyListRequest(dailyList, requestXml, "XHB", "12345");

        OffsetDateTime publishedTime = postDailyListRequest.getPublishedTs();
        assertEquals("2024-04-23T15:00+01:00", publishedTime.toString());
    }

    @Test
    void testPublishedTimeGmtWithTimeZone() throws Exception {
        String requestXml = TestUtils.getContentsFromFile(
            "Tests/dailylist/DailyListXmlRequestMapperTest/publishedDateGmtWithTimeZone.xml");
        DailyListStructure dailyList = xmlParser.unmarshal(requestXml, DailyListStructure.class);
        PostDailyListRequest postDailyListRequest = DailyListXmlRequestMapper.mapToPostDailyListRequest(dailyList, requestXml, "XHB", "12345");

        OffsetDateTime publishedTime = postDailyListRequest.getPublishedTs();
        assertEquals("2024-01-12T11:00Z", publishedTime.toString());
    }

    @Test
    void testPublishedTimeGmtWithoutTimeZone() throws Exception {
        String requestXml = TestUtils.getContentsFromFile(
            "Tests/dailylist/DailyListXmlRequestMapperTest/publishedDateGmtWithoutTimeZone.xml");
        DailyListStructure dailyList = xmlParser.unmarshal(requestXml, DailyListStructure.class);
        PostDailyListRequest postDailyListRequest = DailyListXmlRequestMapper.mapToPostDailyListRequest(dailyList, requestXml, "XHB", "12345");

        OffsetDateTime publishedTime = postDailyListRequest.getPublishedTs();
        assertEquals("2024-01-12T11:00Z", publishedTime.toString());
    }
}
