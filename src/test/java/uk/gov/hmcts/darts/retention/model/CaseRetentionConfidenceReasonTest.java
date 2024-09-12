package uk.gov.hmcts.darts.retention.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;

import java.util.List;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

@Slf4j
@SuppressWarnings("checkstyle:LineLength")
class CaseRetentionConfidenceReasonTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @Test
    void getCaseRetentionConfidenceReason() throws JsonProcessingException {

        CaseRetentionConfidenceReason caseRetentionConfidenceReason = CaseRetentionConfidenceReason.builder()
            .retentionConfidenceAppliedTimestamp("2024-06-26 12:10:00")
            .retentionCases(List.of(buildRetentionCase("Swansea",
                                                       "T1234558",
                                                       "2024-06-26 12:10:00",
                                                       "AGED_CASE"),
                                    buildRetentionCase("Swansea",
                                                       "T12341234",
                                                       "2024-06-26 12:10:00",
                                                       "AGED_CASE")))
            .build();

        String expectedResponse = """
            {
                "ret_conf_applied_ts": "2024-06-26 12:10:00",
                "cases": [
                {
                    "courthouse": "Swansea",
                    "case_number": "T1234558",
                    "ret_conf_updated_ts": "2024-06-26 12:10:00",
                    "ret_conf_reason": "AGED_CASE"
                },
                {
                    "courthouse": "Swansea",
                    "case_number": "T12341234",
                    "ret_conf_updated_ts": "2024-06-26 12:10:00",
                    "ret_conf_reason": "AGED_CASE"
                }
              ]
            }
            """;

        String actualResponse = objectMapper.writeValueAsString(caseRetentionConfidenceReason);
        log.info("actual Response {}", actualResponse);
        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

        String escapedActualResponse = StringEscapeUtils.escapeJson(actualResponse);
        String escapedExpectedResponse = "{\\\"ret_conf_applied_ts\\\":\\\"2024-06-26 12:10:00\\\",\\\"cases\\\":[{\\\"courthouse\\\":\\\"Swansea\\\",\\\"case_number\\\":\\\"T1234558\\\",\\\"ret_conf_updated_ts\\\":\\\"2024-06-26 12:10:00\\\",\\\"ret_conf_reason\\\":\\\"AGED_CASE\\\"},{\\\"courthouse\\\":\\\"Swansea\\\",\\\"case_number\\\":\\\"T12341234\\\",\\\"ret_conf_updated_ts\\\":\\\"2024-06-26 12:10:00\\\",\\\"ret_conf_reason\\\":\\\"AGED_CASE\\\"}]}";
        log.info("escapedActualResponse   {}", escapedActualResponse);
        log.info("escapedExpectedResponse {}", escapedExpectedResponse);
        Assertions.assertEquals(escapedExpectedResponse, escapedActualResponse);

    }

    @Test
    void getCaseRetentionConfidenceReasonWithNullReason() throws JsonProcessingException {

        CaseRetentionConfidenceReason caseRetentionConfidenceReason = CaseRetentionConfidenceReason.builder()
            .retentionConfidenceAppliedTimestamp("2024-06-26 12:10:00")
            .retentionCases(List.of(buildRetentionCase("Swansea",
                                                       "T1234558",
                                                       "2024-06-26 12:10:00",
                                                       null),
                                    buildRetentionCase("Swansea",
                                                       "T12341234",
                                                       "2024-06-26 12:10:00",
                                                       null)))
            .build();

        String expectedResponse = """
            {
                "ret_conf_applied_ts": "2024-06-26 12:10:00",
                "cases": [
                {
                    "courthouse": "Swansea",
                    "case_number": "T1234558",
                    "ret_conf_updated_ts": "2024-06-26 12:10:00"
                },
                {
                    "courthouse": "Swansea",
                    "case_number": "T12341234",
                    "ret_conf_updated_ts": "2024-06-26 12:10:00"
                }
              ]
            }
            """;

        String actualResponse = objectMapper.writeValueAsString(caseRetentionConfidenceReason);
        log.info("actual Response {}", actualResponse);
        log.info("expect Response {}", expectedResponse);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

        String escapedActualResponse = StringEscapeUtils.escapeJson(actualResponse);
        String escapedExpectedResponse = "{\\\"ret_conf_applied_ts\\\":\\\"2024-06-26 12:10:00\\\",\\\"cases\\\":[{\\\"courthouse\\\":\\\"Swansea\\\",\\\"case_number\\\":\\\"T1234558\\\",\\\"ret_conf_updated_ts\\\":\\\"2024-06-26 12:10:00\\\"},{\\\"courthouse\\\":\\\"Swansea\\\",\\\"case_number\\\":\\\"T12341234\\\",\\\"ret_conf_updated_ts\\\":\\\"2024-06-26 12:10:00\\\"}]}";
        log.info("escapedActualResponse   {}", escapedActualResponse);
        log.info("escapedExpectedResponse {}", escapedExpectedResponse);
        Assertions.assertEquals(escapedExpectedResponse, escapedActualResponse);

    }

    private static CaseRetentionConfidenceReason.RetentionCase buildRetentionCase(String courthouse,
                                                                                  String caseNumber,
                                                                                  String retentionConfidenceUpdatedTimestamp,
                                                                                  String retentionConfidenceReason) {
        return CaseRetentionConfidenceReason.RetentionCase.builder()
            .courthouse(courthouse)
            .caseNumber(caseNumber)
            .retentionConfidenceUpdatedTimestamp(retentionConfidenceUpdatedTimestamp)
            .retentionConfidenceReason(retentionConfidenceReason)
            .build();
    }
}