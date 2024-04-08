package uk.gov.hmcts.darts.dailylist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.DailyListPatchRequest;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;

class DailyListServiceTest extends IntegrationBase {

    static final String CPP = "CPP";
    static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    DailyListService service;

    @Autowired
    CourthouseRepository courthouseRepository;

    @Autowired
    DailyListRepository dailyListRepository;

    @MockBean
    UserIdentity mockUserIdentity;

    @BeforeAll
    static void beforeAll() {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void insert1OkJson() throws IOException {
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");
        String dailyListJsonStr = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/insert1_ok/DailyListRequest.json");
        DailyListJsonObject dailyList = MAPPER.readValue(dailyListJsonStr, DailyListJsonObject.class);
        DailyListPostRequest request = new DailyListPostRequest(CPP, null, null, null, null, null, dailyList, "some-message-id");

        service.saveDailyListToDatabase(request);

        List<DailyListEntity> resultList = dailyListRepository.findAll();
        assertEquals(1, resultList.size());
        DailyListEntity dailyListEntity = resultList.get(0);

        String expectedResponseLocation = "tests/dailylist/DailyListServiceTest/insert1_ok/expectedResponse.json";
        checkExpectedResponse(dailyListEntity, expectedResponseLocation);
    }

    @Test
    void insert1OkJsonAndXml() throws IOException {
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");
        String dailyListJsonStr = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/insert1OkJsonAndXml/DailyListRequest.json");
        DailyListJsonObject dailyList = MAPPER.readValue(dailyListJsonStr, DailyListJsonObject.class);
        DailyListPostRequest request = new DailyListPostRequest(CPP, null, null, "someXml", null, null, dailyList, "some-message-id");

        service.saveDailyListToDatabase(request);

        List<DailyListEntity> resultList = dailyListRepository.findAll();
        assertEquals(1, resultList.size());
        DailyListEntity dailyListEntity = resultList.get(0);

        String expectedResponseLocation = "tests/dailylist/DailyListServiceTest/insert1OkJsonAndXml/expectedResponse.json";
        checkExpectedResponse(dailyListEntity, expectedResponseLocation);
    }

    @Test
    void updateOkJsonWithXml() throws IOException {
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");
        String dailyListJsonStr = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/insert1OkJsonAndXml/DailyListRequest.json");
        DailyListJsonObject dailyList = MAPPER.readValue(dailyListJsonStr, DailyListJsonObject.class);
        DailyListPostRequest request = new DailyListPostRequest(CPP, null, null, null, null, null, dailyList, "some-message-id");
        service.saveDailyListToDatabase(request);

        request = new DailyListPostRequest(CPP, null, null, "someXml", null, null, dailyList, "some-message-id");
        service.saveDailyListToDatabase(request);

        List<DailyListEntity> resultList = dailyListRepository.findAll();
        assertEquals(1, resultList.size());
        DailyListEntity dailyListEntity = resultList.get(0);

        String expectedResponseLocation = "tests/dailylist/DailyListServiceTest/insert1OkJsonAndXml/expectedResponse.json";
        checkExpectedResponse(dailyListEntity, expectedResponseLocation);
    }

    @Test
    void insert1DuplicateOk() throws IOException {
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");

        String requestBody = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/insert1_duplicate_ok/DailyListRequest.json");
        DailyListJsonObject dailyList = MAPPER.readValue(requestBody, DailyListJsonObject.class);

        DailyListPostRequest request = new DailyListPostRequest(CPP, null, null, null, null, null, dailyList, "some-message-id");
        service.saveDailyListToDatabase(request);

        String requestBody2 = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/insert1_duplicate_ok/DailyListRequest2.json");
        DailyListJsonObject dailyList2 = MAPPER.readValue(requestBody2, DailyListJsonObject.class);

        DailyListPostRequest request2 = new DailyListPostRequest(CPP, null, null, null, null, null, dailyList2, "some-message-id");
        service.saveDailyListToDatabase(request2);
        List<DailyListEntity> resultList = dailyListRepository.findAll();
        assertEquals(1, resultList.size());
        DailyListEntity dailyListEntity = resultList.get(0);

        checkExpectedResponse(
            dailyListEntity,
            "tests/dailylist/DailyListServiceTest/insert1_duplicate_ok/expectedResponse.json"
        );
    }

    private void checkExpectedResponse(DailyListEntity dailyListEntity, String expectedResponseLocation) throws IOException {
        dailyListEntity.setCreatedDateTime(null);
        dailyListEntity.setLastModifiedDateTime(null);
        dailyListEntity.setId(null);
        String actualResponse = MAPPER.writeValueAsString(dailyListEntity);
        String expectedResponse = getContentsFromFile(expectedResponseLocation);
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void ok_saveDl_xml_then_json() throws IOException {
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");

        DailyListPostRequest requestWithXml = new DailyListPostRequest(
            CPP,
            "SWANSEA",
            LocalDate.now(),
            "theXml",
            "uniqueId",
            OffsetDateTime.now(),
            null,
            "some-message-id"
        );

        service.saveDailyListToDatabase(requestWithXml);
        DailyListEntity dailyListFromDb = getDailyListFromDb();
        assertThat(dailyListFromDb.getUniqueId(), equalTo("uniqueId"));
        assertThat(dailyListFromDb.getStartDate(), equalTo(LocalDate.now()));
        assertThat(dailyListFromDb.getSource(), equalTo("CPP"));
        assertThat(dailyListFromDb.getListingCourthouse(), equalTo("SWANSEA"));
        assertThat(dailyListFromDb.getXmlContent(), equalTo("theXml"));
        assertThat(dailyListFromDb.getContent(), nullValue());


        String dailyListJson = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/ok_saveDl_xml_then_json/document.json");
        DailyListJsonObject dailyListJsonObject = MAPPER.readValue(dailyListJson, DailyListJsonObject.class);
        DailyListPatchRequest dailyListPatchRequest = new DailyListPatchRequest(
            dailyListFromDb.getId(),
            dailyListJsonObject
        );
        service.updateDailyListInDatabase(dailyListPatchRequest);

        dailyListFromDb = getDailyListFromDb();
        assertThat(dailyListFromDb.getUniqueId(), equalTo("CSDDL1613756980160"));
        assertThat(dailyListFromDb.getStartDate(), equalTo(LocalDate.of(2021, 2, 23)));
        assertThat(dailyListFromDb.getSource(), equalTo("CPP"));
        assertThat(dailyListFromDb.getListingCourthouse(), equalTo("SWANSEA"));
        assertThat(dailyListFromDb.getXmlContent(), equalTo("theXml"));
        assertThat(dailyListFromDb.getContent(), containsString("DailyList_457_20210219174938.xml"));
    }

    private DailyListEntity getDailyListFromDb() {
        List<DailyListEntity> resultList = dailyListRepository.findAll();
        return resultList.get(0);
    }
}
