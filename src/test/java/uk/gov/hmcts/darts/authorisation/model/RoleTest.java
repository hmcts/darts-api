package uk.gov.hmcts.darts.authorisation.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RoleTest {

    Role role;

    @Test
    void getAndSetRoleId() {
        role = Role.builder().build();
        assertNull(role.getRoleId());

        int newRoleId = 10;
        role.setRoleId(newRoleId);
        assertEquals(newRoleId, role.getRoleId());
    }

    @Test
    void getAndSetRoleName() {
        role = Role.builder().build();
        assertNull(role.getRoleName());

        String newRoleName = "Role";
        role.setRoleName(newRoleName);
        assertEquals(newRoleName, role.getRoleName());
    }

    @Test
    void getAndSetPermissions() {
        role = Role.builder().build();
        assertNull(role.getRoleName());

        List<Permission> newPermissions = new ArrayList<>();
        newPermissions.add(Permission.builder().build());
        newPermissions.add(Permission.builder().build());
        role.setPermissions(newPermissions);

        List<Permission> permissionsResult = role.getPermissions();
        assertEquals(newPermissions, permissionsResult);
        assertEquals(2, permissionsResult.size());
    }
}
