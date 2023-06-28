package uk.gov.hmcts.darts.cases.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCasesMapperTest {
    ObjectMapper objectMapper;

    @BeforeAll
    void beforeAll() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @Test
    void testOk() throws IOException {

        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(5);

        List<ScheduledCase> scheduledCases = GetCasesMapper.mapToCourtCases(hearings);

        String actualResponse = objectMapper.writeValueAsString(scheduledCases);
        String expectedResponse = getContentsFromFile("Tests/cases/GetCasesMapperTest/testOk/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void testOrderedByTime() throws IOException {

        List<HearingEntity> hearingList = new ArrayList<>();
        int counter = 1;
        String caseNumString = "caseNum_";
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter++, LocalTime.of(9, 0, 0)));
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter++, LocalTime.of(18, 0, 0)));
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter++, LocalTime.of(4, 0, 0)));
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter++, LocalTime.of(15, 0, 0)));
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter++, LocalTime.of(12, 0, 0)));
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter++, LocalTime.of(10, 0, 0)));
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter, LocalTime.of(16, 0, 0)));

        List<ScheduledCase> scheduledCases = GetCasesMapper.mapToCourtCases(hearingList);

        String actualResponse = objectMapper.writeValueAsString(scheduledCases);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/GetCasesMapperTest/testOrderedByTime/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

}
