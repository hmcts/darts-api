package uk.gov.hmcts.darts.authorisation.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PermissionTest {

    Permission permission;

    @Test
    void getAndSetPermissionId() {
        permission = Permission.builder().build();
        Assertions.assertNull(permission.getPermissionId());

        int newPermissionId = 10;
        permission.setPermissionId(newPermissionId);
        Assertions.assertEquals(newPermissionId, permission.getPermissionId());
    }

    @Test
    void getAndSetPermissionName() {
        permission = Permission.builder().build();
        Assertions.assertNull(permission.getPermissionName());

        String newPermissionName = "Permission";
        permission.setPermissionName(newPermissionName);
        Assertions.assertEquals(newPermissionName, permission.getPermissionName());
    }

    @Test
    void builder() {
        Integer permissionId = 123;
        String permissionName = "TestPermission";
        permission = Permission.builder()
            .permissionId(permissionId)
            .permissionName(permissionName)
            .build();

        Assertions.assertEquals(permissionId, permission.getPermissionId());
        Assertions.assertEquals(permissionName, permission.getPermissionName());
    }
}
