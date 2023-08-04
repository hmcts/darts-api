package uk.gov.hmcts.darts.cases.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdvancedSearchResponseMapperTest {

    public static final String TEST_1 = "test1";
    ObjectMapper objectMapper;

    @BeforeAll
    void beforeAll() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @Test
    void empty() {
        List<HearingEntity> hearings = new ArrayList<>();
        List<AdvancedSearchResult> result = AdvancedSearchResponseMapper.mapResponse(hearings);
        assertEquals(0, result.size());
    }

    @Test
    void one() throws IOException {
        List<HearingEntity> hearings = new ArrayList<>();
        hearings.add(CommonTestDataUtil.createHearing(TEST_1, LocalTime.NOON));
        List<AdvancedSearchResult> result = AdvancedSearchResponseMapper.mapResponse(hearings);

        String actualResponse = objectMapper.writeValueAsString(result);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/AdvancedSearchResponseMapperTest/one/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void twoSameCase() throws IOException {
        HearingEntity hearing1 = new HearingEntity();
        hearing1.setCourtCase(CommonTestDataUtil.createCase(TEST_1));
        hearing1.setCourtroom(CommonTestDataUtil.createCourtroom("1"));
        hearing1.setHearingDate(LocalDate.of(2023, 6, 20));
        hearing1.setScheduledStartTime(LocalTime.NOON);
        hearing1.setId(201);
        hearing1.setJudges(CommonTestDataUtil.createJudges(2));

        HearingEntity hearing2 = new HearingEntity();
        hearing2.setCourtCase(CommonTestDataUtil.createCase(TEST_1));
        hearing2.setCourtroom(CommonTestDataUtil.createCourtroom("2"));
        hearing2.setHearingDate(LocalDate.of(2023, 6, 21));
        hearing2.setScheduledStartTime(LocalTime.NOON);
        hearing2.setId(202);
        hearing2.setJudges(CommonTestDataUtil.createJudges(3));


        List<HearingEntity> hearings = new ArrayList<>();
        hearings.add(hearing1);
        hearings.add(hearing2);
        List<AdvancedSearchResult> result = AdvancedSearchResponseMapper.mapResponse(hearings);

        String actualResponse = objectMapper.writeValueAsString(result);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/AdvancedSearchResponseMapperTest/twoSameCase/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void fourWithTwoSameCase() throws IOException {
        HearingEntity hearing1 = new HearingEntity();
        hearing1.setCourtCase(CommonTestDataUtil.createCaseWithId(TEST_1, 101));
        hearing1.setCourtroom(CommonTestDataUtil.createCourtroom("1"));
        hearing1.setHearingDate(LocalDate.of(2023, 6, 20));
        hearing1.setScheduledStartTime(LocalTime.of(11, 0));
        hearing1.setId(201);
        hearing1.setJudges(CommonTestDataUtil.createJudges(2));

        HearingEntity hearing2 = new HearingEntity();
        hearing2.setCourtCase(CommonTestDataUtil.createCaseWithId(TEST_1, 101));
        hearing2.setCourtroom(CommonTestDataUtil.createCourtroom("2"));
        hearing2.setHearingDate(LocalDate.of(2023, 6, 21));
        hearing2.setScheduledStartTime(LocalTime.of(9, 0));
        hearing2.setId(202);
        hearing2.setJudges(CommonTestDataUtil.createJudges(3));


        HearingEntity hearing3 = new HearingEntity();
        hearing3.setCourtCase(CommonTestDataUtil.createCaseWithId("test2", 102));
        hearing3.setCourtroom(CommonTestDataUtil.createCourtroom("2"));
        hearing3.setHearingDate(LocalDate.of(2023, 6, 22));
        hearing3.setScheduledStartTime(LocalTime.of(10, 0));
        hearing3.setId(203);
        hearing3.setJudges(CommonTestDataUtil.createJudges(4));


        HearingEntity hearing4 = new HearingEntity();
        hearing4.setCourtCase(CommonTestDataUtil.createCaseWithId("test3", 103));
        hearing4.setCourtroom(CommonTestDataUtil.createCourtroom("2"));
        hearing4.setHearingDate(LocalDate.of(2023, 6, 23));
        hearing4.setScheduledStartTime(LocalTime.of(13, 0));
        hearing4.setId(204);
        hearing4.setJudges(CommonTestDataUtil.createJudges(5));


        List<HearingEntity> hearings = new ArrayList<>();
        hearings.add(hearing1);
        hearings.add(hearing2);
        hearings.add(hearing3);
        hearings.add(hearing4);
        List<AdvancedSearchResult> result = AdvancedSearchResponseMapper.mapResponse(hearings);

        String actualResponse = objectMapper.writeValueAsString(result);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/AdvancedSearchResponseMapperTest/fourWithTwoSameCase/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

}
