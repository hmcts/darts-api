package uk.gov.hmcts.darts.usermanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.model.Problem;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndTimestamps;

import java.util.Set;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.PredefinedPrimaryKeys.TEST_JUDGE_GLOBAL_SECURITY_GROUP_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;

@AutoConfigureMockMvc
class UserControllerGetUsersByIdIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/users/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private UserAccountStub accountStub;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    @Test
    void usersGetShouldReturnOk() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        UserAccountEntity accountEntity = accountStub.createJudgeUser();

        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL + accountEntity.getId()))
            .andExpect(status().isOk())
            .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        UserWithIdAndTimestamps userWithIdAndTimestamps =
            mapper.readValue(mvcResult.getResponse().getContentAsString(), UserWithIdAndTimestamps.class);

        Assertions.assertEquals("Judgedefault@example.com", userWithIdAndTimestamps.getEmailAddress());
        Assertions.assertTrue(userWithIdAndTimestamps.getActive());
        Assertions.assertEquals("JudgedefaultFullName", userWithIdAndTimestamps.getFullName());
        Assertions.assertEquals(TEST_JUDGE_GLOBAL_SECURITY_GROUP_ID, userWithIdAndTimestamps.getSecurityGroupIds().getFirst());

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN, SUPER_USER));
        verify(mockUserIdentity, atLeastOnce()).getUserIdFromJwt();//Called by AuditorRevisionListener
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void usersGetShouldReturnSystemUserFailure() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        UserAccountEntity userAccountEntity = accountStub.getSystemUserAccountEntity();

        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL + userAccountEntity.getId()))
            .andReturn();

        Problem problem = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), Problem.class);

        Assertions.assertEquals(UserManagementError.USER_NOT_FOUND.getHttpStatus().value(), mvcResult.getResponse().getStatus());
        Assertions.assertEquals(UserManagementError.USER_NOT_FOUND.getErrorTypeNumeric(), problem.getType().toString());
        Assertions.assertEquals("User id " + userAccountEntity.getId() + " not found", problem.getDetail());
        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN, SUPER_USER));
        verify(mockUserIdentity, atLeastOnce()).getUserIdFromJwt();//Called by AuditorRevisionListener
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void usersGetShouldReturnForbiddenError() throws Exception {
        superAdminUserStub.givenUserIsNotAuthorised(mockUserIdentity);

        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL + "1"))
            .andExpect(status().isForbidden())
            .andReturn();

        String expectedResponse = """
            {"type":"AUTHORISATION_109",
            "title":"User is not authorised for this endpoint",
            "status":403}
            """;
        JSONAssert.assertEquals(
            expectedResponse,
            mvcResult.getResponse().getContentAsString(),
            JSONCompareMode.NON_EXTENSIBLE
        );

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN, SUPER_USER));
        verify(mockUserIdentity, atLeastOnce()).getUserIdFromJwt();//Called by AuditorRevisionListener
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void usersGetShouldReturnNotFoundError() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL + "123456"))
            .andExpect(status().isNotFound())
            .andReturn();

        String expectedResponse = """
            {"type":"USER_MANAGEMENT_100",
            "title":"The provided user does not exist",
            "status":404,
            "detail":"User id 123456 not found"}
            """;
        JSONAssert.assertEquals(
            expectedResponse,
            mvcResult.getResponse().getContentAsString(),
            JSONCompareMode.NON_EXTENSIBLE
        );

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN, SUPER_USER));
        verify(mockUserIdentity, atLeastOnce()).getUserIdFromJwt();//Called by AuditorRevisionListener
        verifyNoMoreInteractions(mockUserIdentity);
    }
}
