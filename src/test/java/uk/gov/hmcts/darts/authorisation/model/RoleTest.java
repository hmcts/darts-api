package uk.gov.hmcts.darts.authorisation.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.COURT_CLERK;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.COURT_MANAGER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;

class RoleTest {

    @Test
    void builder() {
        Role role = Role.builder()
            .roleId(COURT_MANAGER.getId())
            .roleName(COURT_MANAGER.toString())
            .permissions(Set.of(
                Permission.builder()
                    .permissionId(2)
                    .permissionName("APPROVE_REJECT_TRANSCRIPTION_REQUEST")
                    .build(),
                Permission.builder()
                    .permissionId(4)
                    .permissionName("LISTEN_TO_AUDIO_FOR_PLAYBACK")
                    .build()
            ))
            .build();

        assertEquals(COURT_MANAGER.getId(), role.getRoleId());
        assertEquals(COURT_MANAGER.toString(), role.getRoleName());
        assertEquals(2, role.getPermissions().size());
    }

    @Test
    void shouldEqualsJudgeRole() {
        Role role = Role.builder()
            .roleId(JUDGE.getId())
            .roleName(JUDGE.toString())
            .permissions(Collections.emptySet())
            .build();

        assertEquals(role, new Role(JUDGE.getId(), JUDGE.toString(), Collections.emptySet()));
    }

    @Test
    void shouldNotEqualsJudgeRole() {
        Role role = Role.builder()
            .roleId(JUDGE.getId())
            .roleName(JUDGE.toString())
            .permissions(Collections.emptySet())
            .build();

        assertNotEquals(role, new Role(COURT_CLERK.getId(), COURT_CLERK.toString(), Collections.emptySet()));
    }

}
