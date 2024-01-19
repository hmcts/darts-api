package uk.gov.hmcts.darts.retention.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@AutoConfigureMockMvc
class RetentionControllerPostRetentionTest extends IntegrationBase {
    @Autowired
    private transient MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;

    public static final String ENDPOINT_URL = "/retentions";

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "12345";

    @BeforeEach
    void setUp() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
                .createAuthorisedIntegrationTestUser(SOME_COURTHOUSE);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }


    @Test
    void happyPath() throws Exception {
        CourtCaseEntity courtCase = dartsDatabase.createCase(
                SOME_COURTHOUSE,
                SOME_CASE_NUMBER
        );
        courtCase.setClosed(true);
        dartsDatabase.save(courtCase);

        OffsetDateTime retainUntilDate = OffsetDateTime.parse("2023-01-01T12:00Z");

        dartsDatabase.createCaseRetentionObject(courtCase, CaseRetentionStatus.COMPLETE, retainUntilDate, false);


        String requestBody = """
                {
                  "case_id": 1,
                  "retention_date": "2024-05-20",
                  "is_permanent_retention": false,
                  "comments": "string"
                }""";

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(requestBody);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    }

}
