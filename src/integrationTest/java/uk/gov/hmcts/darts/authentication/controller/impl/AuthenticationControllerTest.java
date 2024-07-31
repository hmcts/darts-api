package uk.gov.hmcts.darts.authentication.controller.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthenticationControllerTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/userstate");

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockBean
    private UserIdentity mockUserIdentity;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetUserStateIsActive() throws Exception {
        UserAccountEntity userAccountEntity = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        MvcResult mvcResult = mockMvc.perform(
            get(ENDPOINT))
        .andExpect(status().isOk())
        .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        UserState userState = objectMapper.readValue(content, UserState.class);

        assertTrue(userState.getIsActive());
        assertEquals(userAccountEntity.getId(), userState.getUserId());
        assertEquals(userAccountEntity.getUserFullName(), userState.getUserName());
        assertEquals(1, userState.getRoles().size());
        assertEquals(SecurityRoleEnum.SUPER_ADMIN.getId(), userState.getRoles().iterator().next().getRoleId());
    }

    @Test
    void testGetUserStateAccountNotActive() throws Exception {
        UserAccountEntity userAccountEntity = superAdminUserStub.givenUserIsAuthorisedButInactive(mockUserIdentity);

        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT))
            .andExpect(status().isOk())
            .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        UserState userState = objectMapper.readValue(content, UserState.class);

        assertFalse(userState.getIsActive());
        assertEquals(userAccountEntity.getId(), userState.getUserId());
        assertEquals(userAccountEntity.getUserFullName(), userState.getUserName());
        assertEquals(1, userState.getRoles().size());
        assertEquals(SecurityRoleEnum.SUPER_ADMIN.getId(), userState.getRoles().iterator().next().getRoleId());
    }
}
