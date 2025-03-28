package uk.gov.hmcts.darts.dailylist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.DailyListPatchRequestInternal;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequestInternal;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.TransactionalUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static java.util.Comparator.comparing;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

class DailyListServiceTest extends IntegrationBase {

    static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    DailyListService service;

    @Autowired
    CourthouseRepository courthouseRepository;

    @Autowired
    DailyListRepository dailyListRepository;
    @Autowired
    TransactionalUtil transactionalUtil;

    @MockitoBean
    UserIdentity mockUserIdentity;

    @BeforeAll
    static void beforeAll() {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void insert1OkJson() throws IOException {
        when(mockUserIdentity.getUserAccount()).thenReturn(dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity());
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");
        String dailyListJsonStr = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/insert1_ok/DailyListRequest.json");
        DailyListJsonObject dailyList = MAPPER.readValue(dailyListJsonStr, DailyListJsonObject.class);
        DailyListPostRequestInternal request = new DailyListPostRequestInternal(SourceType.CPP.toString(), null, null, null, null, null, dailyList,
                                                                                "some-message-id");

        service.saveDailyListToDatabase(request);

        List<DailyListEntity> resultList = dailyListRepository.findAll();
        assertEquals(1, resultList.size());
        DailyListEntity dailyListEntity = resultList.getFirst();

        String expectedResponseLocation = "tests/dailylist/DailyListServiceTest/insert1_ok/expectedResponse.json";
        checkExpectedResponse(dailyListEntity, expectedResponseLocation);
    }

    @Test
    void insert1OkJsonAndXml() throws IOException {
        when(mockUserIdentity.getUserAccount()).thenReturn(dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity());
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");
        String dailyListJsonStr = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/insert1OkJsonAndXml/DailyListRequest.json");
        DailyListJsonObject dailyList = MAPPER.readValue(dailyListJsonStr, DailyListJsonObject.class);
        DailyListPostRequestInternal request = new DailyListPostRequestInternal(SourceType.CPP.toString(), null, null, "someXml", null, null, dailyList,
                                                                                "some-message-id");

        service.saveDailyListToDatabase(request);

        List<DailyListEntity> resultList = dailyListRepository.findAll();
        assertEquals(1, resultList.size());
        DailyListEntity dailyListEntity = resultList.getFirst();

        String expectedResponseLocation = "tests/dailylist/DailyListServiceTest/insert1OkJsonAndXml/expectedResponse.json";
        checkExpectedResponse(dailyListEntity, expectedResponseLocation);
    }

    @Test
    void updateOkJsonWithXml() throws IOException {
        when(mockUserIdentity.getUserAccount()).thenReturn(dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity());
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");
        String dailyListJsonStr = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/insert1OkJsonAndXml/DailyListRequest.json");
        DailyListJsonObject dailyList = MAPPER.readValue(dailyListJsonStr, DailyListJsonObject.class);
        DailyListPostRequestInternal request = new DailyListPostRequestInternal(SourceType.CPP.toString(), null, null, null, null, null, dailyList,
                                                                                "some-message-id");
        service.saveDailyListToDatabase(request);

        request = new DailyListPostRequestInternal(SourceType.CPP.toString(), null, null, "someXml", null, null, dailyList, "some-message-id");
        service.saveDailyListToDatabase(request);

        var resultList = dailyListRepository.findAll().stream()
            .sorted(comparing(DailyListEntity::getId).reversed())
            .toList();

        assertEquals(2, resultList.size());

        String expectedResponseLocation1 = "tests/dailylist/DailyListServiceTest/insert1OkJsonAndXml/expectedResponse.json";
        String expectedResponseLocation2 = "tests/dailylist/DailyListServiceTest/insert1OkJsonAndXml/expectedResponse2.json";
        checkExpectedResponse(resultList.getFirst(), expectedResponseLocation1);
        checkExpectedResponse(resultList.get(1), expectedResponseLocation2);
    }

    @Test
    void insert1DuplicateOk() throws IOException {
        when(mockUserIdentity.getUserAccount()).thenReturn(dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity());
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");

        String requestBody = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/insert1_duplicate_ok/DailyListRequest.json");
        DailyListJsonObject dailyList = MAPPER.readValue(requestBody, DailyListJsonObject.class);

        DailyListPostRequestInternal request = new DailyListPostRequestInternal(SourceType.CPP.toString(), null, null, null, null, null, dailyList,
                                                                                "some-message-id");
        service.saveDailyListToDatabase(request);

        String requestBody2 = getContentsFromFile(
            "tests/dailylist/DailyListServiceTest/insert1_duplicate_ok/DailyListRequest2.json");
        DailyListJsonObject dailyList2 = MAPPER.readValue(requestBody2, DailyListJsonObject.class);

        DailyListPostRequestInternal request2 = new DailyListPostRequestInternal(SourceType.CPP.toString(), null, null, null, null, null, dailyList2,
                                                                                 "some-message-id");
        service.saveDailyListToDatabase(request2);
        List<DailyListEntity> resultList = dailyListRepository.findAll();
        assertEquals(2, resultList.size());
        DailyListEntity dailyListEntity = resultList.getFirst();
        checkExpectedResponse(
            dailyListEntity,
            "tests/dailylist/DailyListServiceTest/insert1_duplicate_ok/expectedResponse.json"
        );


    }

    @SneakyThrows
    private void checkExpectedResponse(DailyListEntity dailyListEntity, String expectedResponseLocation) {
        dailyListEntity.setCreatedDateTime(null);
        dailyListEntity.setLastModifiedDateTime(null);
        dailyListEntity.setCreatedBy(null);
        dailyListEntity.setLastModifiedBy(null);
        dailyListEntity.setId(null);
        String actualResponse = MAPPER.writeValueAsString(dailyListEntity);
        String expectedResponse = getContentsFromFile(expectedResponseLocation);
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void ok_saveDl_xml_then_json() throws IOException {
        dartsDatabase.createCourthouseWithNameAndCode("SWANSEA", 457, "Swansea");
        when(mockUserIdentity.getUserAccount()).thenReturn(dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity());
        DailyListPostRequestInternal requestWithXml = new DailyListPostRequestInternal(
            SourceType.CPP.toString(),
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
        DailyListPatchRequestInternal dailyListPatchRequest = new DailyListPatchRequestInternal(
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
        return resultList.getFirst();
    }
}