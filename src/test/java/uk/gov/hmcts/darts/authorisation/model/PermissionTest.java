package uk.gov.hmcts.darts.authorisation.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PermissionTest {

    Permission permission;

    @Test
    void getAndSetPermissionId() {
        permission = Permission.builder().build();
        assertNull(permission.getPermissionId());

        int newPermissionId = 10;
        permission.setPermissionId(newPermissionId);
        assertEquals(newPermissionId, permission.getPermissionId());
    }

    @Test
    void getAndSetPermissionName() {
        permission = Permission.builder().build();
        assertNull(permission.getPermissionName());

        String newPermissionName = "Permission";
        permission.setPermissionName(newPermissionName);
        assertEquals(newPermissionName, permission.getPermissionName());
    }

    @Test
    void builder() {
        Integer permissionId = 123;
        String permissionName = "TestPermission";
        permission = Permission.builder()
            .permissionId(permissionId)
            .permissionName(permissionName)
            .build();

        assertEquals(permissionId, permission.getPermissionId());
        assertEquals(permissionName, permission.getPermissionName());
    }
}
