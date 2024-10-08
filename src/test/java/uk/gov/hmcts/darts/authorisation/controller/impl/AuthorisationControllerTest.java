package uk.gov.hmcts.darts.authorisation.controller.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.authorisation.model.UserStateRole;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;

@ExtendWith(MockitoExtension.class)
class AuthorisationControllerTest {
    @InjectMocks
    private AuthorisationControllerImpl controller;

    @Mock
    private AuthorisationApi authorisationApi;

    @Test
    void getUserStateOk() {
        UserAccountEntity userAccountEntity = CommonTestDataUtil.createUserAccountWithId();
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
            .userName("testUsername")
            .roles(newRoles)
            .isActive(true)
            .build();
        when(authorisationApi.getAuthorisation(userAccountEntity.getId())).thenReturn(userState);

        UserState userStateResponse = controller.getUserState();

        assertNotNull(userStateResponse);
        assertEquals("testUsername", userStateResponse.getUserName());
        assertTrue(userStateResponse.getIsActive());
    }

    @Test
    void getUserStateForbiddenWhenNoEmailAddress() {
        when(authorisationApi.getCurrentUser()).thenThrow(new DartsApiException(AuthorisationError.USER_DETAILS_INVALID));
        var exception = assertThrows(DartsApiException.class, () -> controller.getUserState());
        assertEquals(AuthorisationError.USER_DETAILS_INVALID, exception.getError());
    }

}