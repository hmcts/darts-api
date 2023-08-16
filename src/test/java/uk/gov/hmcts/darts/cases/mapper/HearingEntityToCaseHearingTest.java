package uk.gov.hmcts.darts.cases.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HearingEntityToCaseHearingTest {

    ObjectMapper objectMapper;

    @BeforeAll
    void beforeAll() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @Test
    void testWorksWithOneHearing() throws Exception {

        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings);

        String actualResponse = objectMapper.writeValueAsString(hearingList);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/HearingEntityToCaseHearingTest/testWithSingleHearing/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void testMappingToMultipleHearings() throws Exception {

        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(5);

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings);

        String actualResponse = objectMapper.writeValueAsString(hearingList);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/HearingEntityToCaseHearingTest/testWithMultipleHearings/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void testWithNoHearings() throws Exception {

        List<HearingEntity> hearings = new ArrayList<>();

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings);

        String actualResponse = objectMapper.writeValueAsString(hearingList);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/HearingEntityToCaseHearingTest/testWithNoHearings/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

}
