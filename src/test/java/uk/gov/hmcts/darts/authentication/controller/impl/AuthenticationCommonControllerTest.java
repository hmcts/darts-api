package uk.gov.hmcts.darts.authentication.controller.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.authorisation.model.UserStateRole;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class AuthenticationCommonControllerTest {
    @InjectMocks
    private AuthenticationCommonControllerImpl controller;

    @Mock
    private AuthorisationApi authorisationApi;

    @Test
    void getUserStateOk() {
        UserAccountEntity userAccountEntity = CommonTestDataUtil.createUserAccount();
        when(authorisationApi.getCurrentUser()).thenReturn(userAccountEntity);

        Set<UserStateRole> newRoles = new HashSet<>();
        newRoles.add(UserStateRole.builder()
                         .roleId(APPROVER.getId())
                         .roleName(APPROVER.toString())
                         .globalAccess(false)
                         .permissions(new HashSet<>())
                         .build());
        newRoles.add(UserStateRole.builder()
                         .roleId(REQUESTER.getId())
                         .roleName(REQUESTER.toString())
                         .globalAccess(false)
                         .permissions(new HashSet<>())
                         .build());

        UserState userState = UserState.builder()
            .userId(123)
            .userName("UserName")
            .roles(newRoles)
            .build();
        when(authorisationApi.getAuthorisation(userAccountEntity.getEmailAddress())).thenReturn(Optional.of(userState));


        UserState userStateResponse = controller.getUserState();

        assertNotNull(userStateResponse);
        assertEquals("UserName", userStateResponse.getUserName());
    }
}
