package uk.gov.hmcts.darts.cases.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.cases.model.AdminCasesSearchResponseItem;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.util.List;

class AdminCasesSearchResponseMapperTest {

    ObjectMapper objectMapper;

    @BeforeEach
    void beforeEach() {
        if (objectMapper == null) {
            ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
            objectMapper = objectMapperConfig.objectMapper();
        }
    }

    @Test
    void happyPath() throws JsonProcessingException {
        CourtCaseEntity case1 = CommonTestDataUtil.createCaseWithId("case1", 101);
        CommonTestDataUtil.createHearingsForCase(case1, 1, 2);
        CourtCaseEntity case2 = CommonTestDataUtil.createCaseWithId("case2", 102);
        CommonTestDataUtil.createHearingsForCase(case2, 2, 3);
        CourtCaseEntity case3 = CommonTestDataUtil.createCaseWithId("case3", 103);
        CommonTestDataUtil.createHearingsForCase(case3, 3, 4);

        List<AdminCasesSearchResponseItem> result = AdminCasesSearchResponseMapper.mapResponse(List.of(case1, case2, case3));
        String actualResponse = objectMapper.writeValueAsString(result);

        String expectedResponse = """
            [
              {
                "id": 101,
                "case_number": "case1",
                "courthouse": {
                  "id": 1001,
                  "display_name": "case_courthouse"
                },
                "courtrooms": [
                  {
                    "id": 1581,
                    "name": "COURTROOM1"
                  }
                ],
                "judges": [
                  "Judge_1",
                  "Judge_2"
                ],
                "defendants": [
                  "defendant_case1_1",
                  "defendant_case1_2"
                ]
              },
              {
                "id": 102,
                "case_number": "case2",
                "courthouse": {
                  "id": 1001,
                  "display_name": "case_courthouse"
                },
                "courtrooms": [
                  {
                    "id": 1581,
                    "name": "COURTROOM1"
                  },
                  {
                    "id": 1582,
                    "name": "COURTROOM2"
                  }
                ],
                "judges": [
                  "Judge_1",
                  "Judge_2"
                ],
                "defendants": [
                  "defendant_case2_1",
                  "defendant_case2_2"
                ]
              },
              {
                "id": 103,
                "case_number": "case3",
                "courthouse": {
                  "id": 1001,
                  "display_name": "case_courthouse"
                },
                "courtrooms": [
                  {
                    "id": 1581,
                    "name": "COURTROOM1"
                  },
                  {
                    "id": 1582,
                    "name": "COURTROOM2"
                  },
                  {
                    "id": 1583,
                    "name": "COURTROOM3"
                  }
                ],
                "judges": [
                  "Judge_1",
                  "Judge_2"
                ],
                "defendants": [
                  "defendant_case3_1",
                  "defendant_case3_2"
                ]
              }
            ]""";
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }


}