package uk.gov.hmcts.darts.authorisation.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;

class RoleTest {

    @Test
    void builder() {
        Role role = Role.builder()
              .roleId(APPROVER.getId())
              .roleName(APPROVER.toString())
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

        assertEquals(APPROVER.getId(), role.getRoleId());
        assertEquals(APPROVER.toString(), role.getRoleName());
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

        assertNotEquals(role, new Role(REQUESTER.getId(), REQUESTER.toString(), Collections.emptySet()));
    }

}
