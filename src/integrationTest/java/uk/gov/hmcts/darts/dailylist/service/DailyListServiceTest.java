package uk.gov.hmcts.darts.dailylist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.dailylist.exception.DailyListException;
import uk.gov.hmcts.darts.dailylist.model.DailyList;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;
import uk.gov.hmcts.darts.dailylist.repository.DailyListRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.createCourthouse;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@ExtendWith(MockitoExtension.class)
class DailyListServiceTest extends IntegrationBase {

    static final String CPP = "CPP";
    static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    DailyListService service;

    @Autowired
    CourthouseRepository courthouseRepository;

    @Autowired
    DailyListRepository dailyListRepository;

    @BeforeAll
    static void beforeAll() {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void insert1Ok() throws IOException {
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457);
        String requestBody = getContentsFromFile("tests/dailylist/DailyListServiceTest/insert1_ok/DailyListRequest.json");
        DailyList dailyList = MAPPER.readValue(requestBody, DailyList.class);
        DailyListPostRequest request = new DailyListPostRequest(CPP, dailyList);

        service.processIncomingDailyList(request);

        List<DailyListEntity> resultList = dailyListRepository.findAll();
        assertEquals(1, resultList.size());
        DailyListEntity dailyListEntity = resultList.get(0);

        String expectedResponseLocation = "tests/dailylist/DailyListServiceTest/insert1_ok/expectedResponse.json";
        checkExpectedResponse(dailyListEntity, expectedResponseLocation);
    }

    @Test
    void insert1DuplicateOk() throws IOException {
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457);

        String requestBody = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/insert1_duplicate_ok/DailyListRequest.json");
        DailyList dailyList = MAPPER.readValue(requestBody, DailyList.class);

        DailyListPostRequest request = new DailyListPostRequest(CPP, dailyList);
        service.processIncomingDailyList(request);

        String requestBody2 = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/insert1_duplicate_ok/DailyListRequest2.json");
        DailyList dailyList2 = MAPPER.readValue(requestBody2, DailyList.class);

        DailyListPostRequest request2 = new DailyListPostRequest(CPP, dailyList2);
        service.processIncomingDailyList(request2);
        List<DailyListEntity> resultList = dailyListRepository.findAll();
        assertEquals(1, resultList.size());
        DailyListEntity dailyListEntity = resultList.get(0);

        checkExpectedResponse(
            dailyListEntity,
            "tests/dailylist/DailyListServiceTest/insert1_duplicate_ok/expectedResponse.json"
        );
    }

    @Test
    void updateCourthouseOk() throws IOException {
        dartsDatabase.save(createCourthouse("TEMP"));
        String requestBody = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/update_courthouse_ok/DailyListRequest.json");
        DailyList dailyList = MAPPER.readValue(requestBody, DailyList.class);

        DailyListPostRequest request = new DailyListPostRequest(CPP, dailyList);
        service.processIncomingDailyList(request);

        assertEquals(9999, dartsDatabase.findCourthouseWithName("TEMP").getCode());
    }

    @Test
    void insert1InvalidCourthouse() throws IOException {
        String requestBody = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/insert1_invalidCourthouse/DailyListRequest.json");
        DailyList dailyList = MAPPER.readValue(requestBody, DailyList.class);

        DailyListPostRequest request = new DailyListPostRequest(CPP, dailyList);
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            service.processIncomingDailyList(request);
        });

        assertThat(exception.getMessage(), containsString("invalid courthouse 'test'"));
    }

    private void checkExpectedResponse(DailyListEntity dailyListEntity, String expectedResponseLocation) {
        try {
            dailyListEntity.setCreatedDate(null);
            dailyListEntity.setModifiedDateTime(null);
            dailyListEntity.setId(null);
            dailyListEntity.getCourthouse().setId(null);
            dailyListEntity.getCourthouse().setCreatedDateTime(null);
            dailyListEntity.getCourthouse().setLastModifiedDateTime(null);
            String actualResponse = MAPPER.writeValueAsString(dailyListEntity);
            String expectedResponse = getContentsFromFile(expectedResponseLocation);
            JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
        } catch (IOException e) {
            throw new DailyListException(e);
        }
    }
}
