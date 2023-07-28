package uk.gov.hmcts.darts.authorisation.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.darts.common.entity.SecurityRoleEnum.COURT_CLERK;
import static uk.gov.hmcts.darts.common.entity.SecurityRoleEnum.JUDGE;

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

        Set<Permission> newPermissions = new HashSet<>();
        newPermissions.add(Permission.builder()
                               .permissionId(2)
                               .permissionName("Approve/Reject Transcription Request")
                               .build());
        newPermissions.add(Permission.builder()
                               .permissionId(4)
                               .permissionName("Listen to Audio for Playback")
                               .build());
        role.setPermissions(newPermissions);

        Set<Permission> permissionsResult = role.getPermissions();
        assertEquals(newPermissions, permissionsResult);
        assertEquals(2, permissionsResult.size());
    }

    @Test
    void shouldEqualsJudgeRole() {
        role = Role.builder()
            .roleId(JUDGE.getId())
            .roleName(JUDGE.toString())
            .build();

        assertEquals(role, new Role(JUDGE.getId(), JUDGE.toString(), Collections.emptySet()));
    }

    @Test
    void shouldNotEqualsJudgeRole() {
        role = Role.builder()
            .roleId(JUDGE.getId())
            .roleName(JUDGE.toString())
            .build();

        assertNotEquals(role, new Role(COURT_CLERK.getId(), COURT_CLERK.toString(), Collections.emptySet()));
    }

}
