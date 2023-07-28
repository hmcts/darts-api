package uk.gov.hmcts.darts.authorisation.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.darts.common.entity.SecurityRoleEnum.COURT_CLERK;
import static uk.gov.hmcts.darts.common.entity.SecurityRoleEnum.COURT_MANAGER;

class UserStateTest {

    UserState userState;

    @Test
    void getAndSetUserId() {
        userState = UserState.builder().build();
        assertNull(userState.getUserId());

        Integer newUserId = 123;
        userState.setUserId(newUserId);
        assertEquals(newUserId, userState.getUserId());
    }

    @Test
    void getUserName() {
        userState = UserState.builder().build();
        assertNull(userState.getUserName());

        String newUserName = "UserName";
        userState.setUserName(newUserName);
        assertEquals(newUserName, userState.getUserName());
    }

    @Test
    void getRoles() {
        userState = UserState.builder().build();
        assertNull(userState.getRoles());

        Set<Role> newRoles = new HashSet<>();
        newRoles.add(Role.builder()
                         .roleId(COURT_MANAGER.getId())
                         .roleName(COURT_MANAGER.toString())
                         .build());
        newRoles.add(Role.builder()
                         .roleId(COURT_CLERK.getId())
                         .roleName(COURT_CLERK.toString())
                         .build());
        userState.setRoles(newRoles);

        Set<Role> roles = userState.getRoles();
        assertEquals(newRoles, roles);
        assertEquals(2, roles.size());
    }

}
