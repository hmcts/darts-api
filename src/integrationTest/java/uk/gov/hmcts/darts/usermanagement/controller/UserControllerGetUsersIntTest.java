package uk.gov.hmcts.darts.usermanagement.controller;

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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;

@AutoConfigureMockMvc
class UserControllerGetUsersIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/users";
    public static final String EMAIL_ADDRESS = "Email-Address";
    public static final String COURTHOUSE_ID = "courthouse_id";

    private static final String ORIGINAL_USERNAME = "James Smith";
    private static final String ORIGINAL_EMAIL_ADDRESS = "james.smith@hmcts.net";
    private static final String ORIGINAL_DESCRIPTION = "A test user";
    private static final boolean ORIGINAL_SYSTEM_USER_FLAG = false;
    private static final OffsetDateTime ORIGINAL_LAST_LOGIN_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime ORIGINAL_LAST_MODIFIED_DATE_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime ORIGINAL_CREATED_DATE_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    @Test
    void usersGetShouldReturnForbiddenError() throws Exception {
        superAdminUserStub.givenUserIsNotAuthorised(mockUserIdentity);

        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL))
            .andExpect(status().isForbidden())
            .andReturn();

        String expectedResponse = """
            {"type":"AUTHORISATION_109","title":"User is not authorised for this endpoint","status":403}
            """;
        JSONAssert.assertEquals(
            expectedResponse,
            mvcResult.getResponse().getContentAsString(),
            JSONCompareMode.NON_EXTENSIBLE
        );

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN, SUPER_USER));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void getUsersAuthorised() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        createEnabledUserAccountEntity(user);

        mockMvc.perform(get(ENDPOINT_URL)
                            .header("Content-Type", "application/json")
                            .header(EMAIL_ADDRESS, "james.smith@hmcts.net"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[0].full_name").value(ORIGINAL_USERNAME))
            .andExpect(jsonPath("$[0].email_address").value(ORIGINAL_EMAIL_ADDRESS))
            .andExpect(jsonPath("$[0].active").value(true))
            .andExpect(jsonPath("$[0].last_login_at").value("2023-10-27T22:00:00Z"))
            .andExpect(jsonPath("$[0].last_modified_at").exists())
            .andExpect(jsonPath("$[0].created_at").exists())
            .andReturn();

        lenient().when(mockUserIdentity.getUserAccount()).thenReturn(user);
        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN, SUPER_USER));

        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void getUsersEmailNotFound() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        createEnabledUserAccountEntity(user);

        MvcResult response = mockMvc.perform(get(ENDPOINT_URL)
                                                 .header(EMAIL_ADDRESS, "james.smith@hmcts.com"))
            .andReturn();

        assertFalse(response.getResponse().getContentAsString().contains("james.smith@hmcts.com"));
        assertEquals(200, response.getResponse().getStatus());
    }

    @Test
    void getUsersNotAuthorised() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsNotAuthorised(mockUserIdentity);
        createEnabledUserAccountEntity(user);

        MvcResult response = mockMvc.perform(get(ENDPOINT_URL)
                                                 .header(EMAIL_ADDRESS, "james.smith@hmcts.net")
                                                 .queryParam(COURTHOUSE_ID, "21"))
            .andReturn();

        assertFalse(response.getResponse().getContentAsString().contains("james.smith@hmcts.net"));
        assertEquals(403, response.getResponse().getStatus());
    }

    private UserAccountEntity createEnabledUserAccountEntity(UserAccountEntity user) {
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setUserFullName(ORIGINAL_USERNAME);
        userAccountEntity.setEmailAddress(ORIGINAL_EMAIL_ADDRESS);
        userAccountEntity.setUserDescription(ORIGINAL_DESCRIPTION);
        userAccountEntity.setActive(true);
        userAccountEntity.setLastLoginTime(ORIGINAL_LAST_LOGIN_TIME);
        userAccountEntity.setLastModifiedDateTime(ORIGINAL_LAST_MODIFIED_DATE_TIME);
        userAccountEntity.setCreatedDateTime(ORIGINAL_CREATED_DATE_TIME);
        userAccountEntity.setIsSystemUser(ORIGINAL_SYSTEM_USER_FLAG);
        userAccountEntity.setCreatedBy(user);
        userAccountEntity.setLastModifiedBy(user);

        return dartsDatabase.getUserAccountRepository()
            .save(userAccountEntity);
    }

}
