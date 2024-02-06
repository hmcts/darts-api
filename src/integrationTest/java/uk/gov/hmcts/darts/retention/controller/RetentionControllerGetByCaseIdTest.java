package uk.gov.hmcts.darts.retention.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RetentionControllerGetByCaseIdTest extends IntegrationBase {

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "1";
    @Autowired
    private transient MockMvc mockMvc;

    @Test
    void testGetRetentionsOk() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
              SOME_CASE_NUMBER,
              SOME_COURTHOUSE,
              SOME_COURTROOM,
              SOME_DATE_TIME.toLocalDate()
        );
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();

        dartsDatabase.createCaseRetention(courtCase);

        var requestBuilder = get(URI.create(String.format("/retentions?case_id=%s", SOME_CASE_NUMBER)));

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath("$[0].retention_last_changed_date", Matchers.is(Matchers.notNullValue())))
              .andExpect(MockMvcResultMatchers.jsonPath("$[0].retention_date", Matchers.is(Matchers.notNullValue())))
              .andExpect(MockMvcResultMatchers.jsonPath("$[0].amended_by", Matchers.is("system")))
              .andExpect(MockMvcResultMatchers.jsonPath("$[0].retention_policy_applied", Matchers.is("Manual")))
              .andExpect(MockMvcResultMatchers.jsonPath("$[0].comments", Matchers.is("a comment")))
              .andExpect(MockMvcResultMatchers.jsonPath("$[0].status", Matchers.is("a_state")))
              .andExpect(MockMvcResultMatchers.jsonPath("$[1].retention_last_changed_date", Matchers.is(Matchers.notNullValue())))
              .andExpect(MockMvcResultMatchers.jsonPath("$[1].retention_date", Matchers.is(Matchers.notNullValue())))
              .andExpect(MockMvcResultMatchers.jsonPath("$[1].amended_by", Matchers.is("system")))
              .andExpect(MockMvcResultMatchers.jsonPath("$[1].retention_policy_applied", Matchers.is("Manual")))
              .andExpect(MockMvcResultMatchers.jsonPath("$[1].comments", Matchers.is("a comment")))
              .andExpect(MockMvcResultMatchers.jsonPath("$[1].status", Matchers.is("b_state")))
              .andExpect(MockMvcResultMatchers.jsonPath("$[2].retention_last_changed_date", Matchers.is(Matchers.notNullValue())))
              .andExpect(MockMvcResultMatchers.jsonPath("$[2].retention_date", Matchers.is(Matchers.notNullValue())))
              .andExpect(MockMvcResultMatchers.jsonPath("$[2].amended_by", Matchers.is("system")))
              .andExpect(MockMvcResultMatchers.jsonPath("$[2].retention_policy_applied", Matchers.is("Manual")))
              .andExpect(MockMvcResultMatchers.jsonPath("$[2].comments", Matchers.is("a comment")))
              .andExpect(MockMvcResultMatchers.jsonPath("$[2].status", Matchers.is("c_state")));
    }

    @Test
    void testCaseDoesNotExist() throws Exception {
        var requestBuilder = get(URI.create(String.format("/retentions?case_id=%s", "500")));

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals("[]", actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }
}
