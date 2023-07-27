package uk.gov.hmcts.darts.authorisation.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

        List<Role> newRoles = new ArrayList<>();
        newRoles.add(Role.builder().build());
        newRoles.add(Role.builder().build());
        userState.setRoles(newRoles);

        List<Role> roles = userState.getRoles();
        assertEquals(newRoles, roles);
        assertEquals(2, roles.size());
    }
}
