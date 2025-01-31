package uk.gov.hmcts.darts.retention.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperUserStub;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DARTS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;

@AutoConfigureMockMvc
class RetentionControllerGetByCaseIdTest extends IntegrationBase {
    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;
    @Autowired
    private SuperUserStub superUserStub;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "some-case-number";

    @Test
    void shouldGetRetentionsOkForSuperAdmin() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        shouldGetRetentionsOk();
    }

    @Test
    void shouldGetRetentionsOkForSuperUser() throws Exception {
        superUserStub.givenUserIsAuthorised(mockUserIdentity);
        shouldGetRetentionsOk();
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void shouldGetRetentionsOk() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();

        List<CaseRetentionEntity> caseRetentionEntityList = dartsDatabase.createCaseRetention(courtCase)
            .stream()
            .map(caseRetentionEntity -> {
                //Refresh to get latest db values
                return dartsDatabase.getDartsPersistence().refresh(caseRetentionEntity);
            })
            .toList();

        assertThat(caseRetentionEntityList).hasSize(3);
        assertThat(caseRetentionEntityList.get(2).getLastModifiedDateTime()).isAfter(caseRetentionEntityList.get(1).getLastModifiedDateTime());
        assertThat(caseRetentionEntityList.get(1).getLastModifiedDateTime()).isAfter(caseRetentionEntityList.get(0).getLastModifiedDateTime());

        var requestBuilder = get(URI.create(String.format("/retentions?case_id=%s", courtCase.getId())));

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].retention_last_changed_date",
                                                      Matchers.is(caseRetentionEntityList.get(2).getLastModifiedDateTime()
                                                                      .format(DateTimeFormatter.ISO_DATE_TIME))))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].retention_date", Matchers.is(Matchers.notNullValue())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].amended_by", Matchers.is("system")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].retention_policy_applied", Matchers.is("Manual")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].comments", Matchers.is("a comment")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].status", Matchers.is("c_state")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].retention_last_changed_date",
                                                      Matchers.is(caseRetentionEntityList.get(1).getLastModifiedDateTime()
                                                                      .format(DateTimeFormatter.ISO_DATE_TIME))))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].retention_date", Matchers.is(Matchers.notNullValue())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].amended_by", Matchers.is("system")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].retention_policy_applied", Matchers.is("Manual")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].comments", Matchers.is("a comment")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].status", Matchers.is("b_state")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].retention_last_changed_date",
                                                      Matchers.is(caseRetentionEntityList.get(0).getLastModifiedDateTime()
                                                                      .format(DateTimeFormatter.ISO_DATE_TIME))))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].retention_date", Matchers.is(Matchers.notNullValue())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].amended_by", Matchers.is("system")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].retention_policy_applied", Matchers.is("Manual")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].comments", Matchers.is("a comment")))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].status", Matchers.is("a_state")))
        ;

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(JUDICIARY, SUPER_ADMIN, SUPER_USER, DARTS));
    }

    @Test
    void testCaseDoesNotExist() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        var requestBuilder = get(URI.create(String.format("/retentions?case_id=%s", "500")));

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals("[]", actualJson, JSONCompareMode.NON_EXTENSIBLE);

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(JUDICIARY, SUPER_ADMIN, SUPER_USER, DARTS));
    }

}
