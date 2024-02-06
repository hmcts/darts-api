package uk.gov.hmcts.darts.authorisation.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionTest {

    @Test
    void builder() {
        Integer permissionId = 123;
        String permissionName = "TestPermission";

        Permission permission = Permission.builder()
              .permissionId(permissionId)
              .permissionName(permissionName)
              .build();

        assertEquals(permissionId, permission.getPermissionId());
        assertEquals(permissionName, permission.getPermissionName());
    }

}
