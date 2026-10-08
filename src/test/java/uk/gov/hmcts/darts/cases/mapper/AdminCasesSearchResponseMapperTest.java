package uk.gov.hmcts.darts.cases.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.cases.model.AdminCasesSearchResponseItem;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.CaseLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

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
    void mapResponse_shouldMapCases_whenLinkedCasesEmpty() throws JsonProcessingException {
        CourtCaseEntity case1 = CommonTestDataUtil.createCaseWithId("case1", 101);
        CommonTestDataUtil.createHearingsForCase(case1, 1, 2);
        CourtCaseEntity case2 = CommonTestDataUtil.createCaseWithId("case2", 102);
        CommonTestDataUtil.createHearingsForCase(case2, 2, 3);
        CourtCaseEntity case3 = CommonTestDataUtil.createCaseWithId("case3", 103);
        case3.setDataAnonymised(true);
        case3.setDataAnonymisedTs(OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        CommonTestDataUtil.createHearingsForCase(case3, 3, 4);

        List<AdminCasesSearchResponseItem> result = AdminCasesSearchResponseMapper.mapResponse(List.of(case1, case2, case3), List.of(), Set.of());
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
                ],
                "is_data_anonymised": false
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
                ],
                "is_data_anonymised": false
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
                ],
                "is_data_anonymised": true,
                "data_anonymised_at": "2024-01-01T00:00:00Z"
              }
            ]""";
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void mapResponse_shouldReturnLinkedCases_whenMultipleCasesLinkedToDifferentCases() {
        CourtCaseEntity case1 = CommonTestDataUtil.createCaseWithId("case1", 101);
        CommonTestDataUtil.createHearingsForCase(case1, 1, 2);
        CourtCaseEntity case2 = CommonTestDataUtil.createCaseWithId("case2", 102);
        CommonTestDataUtil.createHearingsForCase(case2, 2, 3);
        CourtCaseEntity linkedCase1 = CommonTestDataUtil.createCaseWithId("linkedCase1", 201);
        CourtCaseEntity linkedCase2 = CommonTestDataUtil.createCaseWithId("linkedCase2", 202);
        CourtCaseEntity linkedCase3 = CommonTestDataUtil.createCaseWithId("linkedCase3", 203);
        linkedCase2.getCourthouse().setId(1002);

        List<AdminCasesSearchResponseItem> result = AdminCasesSearchResponseMapper.mapResponse(
            List.of(case1, case2),
            List.of(
                createLinkedCase(case1, linkedCase1),
                createLinkedCase(case1, linkedCase2),
                createLinkedCase(linkedCase3, case2)
            ),
            Set.of(1001)
        );

        AdminCasesSearchResponseItem mappedCase1 = result.getFirst();
        AdminCasesSearchResponseItem mappedCase2 = result.get(1);

        assertThat(mappedCase1.getLinkedCases())
            .extracting(linkedCase -> linkedCase.getCaseNumber(), linkedCase -> linkedCase.getActiveLink())
            .containsExactlyInAnyOrder(tuple("linkedCase1", true), tuple("linkedCase2", false));
        assertThat(mappedCase2.getLinkedCases())
            .extracting(linkedCase -> linkedCase.getCaseNumber(), linkedCase -> linkedCase.getActiveLink())
            .containsExactly(tuple("linkedCase3", true));
    }

    private static CaseLinkedCaseEntity createLinkedCase(CourtCaseEntity courtCase, CourtCaseEntity linkedCase) {
        CaseLinkedCaseEntity linkedCaseEntity = new CaseLinkedCaseEntity();
        linkedCaseEntity.setCourtCase1(courtCase);
        linkedCaseEntity.setCourtCase2(linkedCase);
        return linkedCaseEntity;
    }

}
