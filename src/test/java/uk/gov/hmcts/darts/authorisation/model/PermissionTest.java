package uk.gov.hmcts.darts.authorisation.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionTest {

    @Test
    void builder() {
        String permissionName = "TestPermission";

        Permission permission = Permission.builder()
            .permissionName(permissionName)
            .build();

        assertEquals(permissionName, permission.getPermissionName());
    }

}
