package uk.gov.hmcts.darts.usermanagement.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AdminUserStub;

import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.ADMIN;

@AutoConfigureMockMvc
class UserControllerGetUsersIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserStub adminUserStub;

    @MockBean
    private UserIdentity mockUserIdentity;

    @Test
    void usersGetShouldReturnForbiddenError() throws Exception {
        adminUserStub.givenUserIsNotAuthorised(mockUserIdentity);

        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL).queryParam("courthouse", "-1"))
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

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void usersGetShouldReturnNotImplemented() throws Exception {
        adminUserStub.givenUserIsAuthorised(mockUserIdentity);

        mockMvc.perform(get(ENDPOINT_URL).queryParam("courthouse", "-1"))
            .andExpect(status().isNotImplemented());

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

}
