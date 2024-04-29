package uk.gov.hmcts.darts.authorisation.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;

class UserStateRoleTest {

    @Test
    void builder() {
        UserStateRole role = UserStateRole.builder()
            .roleId(APPROVER.getId())
            .roleName(APPROVER.toString())
            .globalAccess(false)
            .permissions(Set.of("APPROVE_REJECT_TRANSCRIPTION_REQUEST", "LISTEN_TO_AUDIO_FOR_PLAYBACK")
            )
            .build();

        assertEquals(APPROVER.getId(), role.getRoleId());
        assertEquals(APPROVER.toString(), role.getRoleName());
        assertEquals(2, role.getPermissions().size());
    }

    @Test
    void shouldEqualsJudgeRole() {
        UserStateRole role = UserStateRole.builder()
            .roleId(JUDGE.getId())
            .roleName(JUDGE.toString())
            .globalAccess(false)
            .permissions(Collections.emptySet())
            .build();

        assertEquals(role, new UserStateRole(JUDGE.getId(), JUDGE.toString(), false, Collections.emptySet(), Collections.emptySet()));
    }

    @Test
    void shouldNotEqualsJudgeRole() {
        UserStateRole role = UserStateRole.builder()
            .roleId(JUDGE.getId())
            .roleName(JUDGE.toString())
            .globalAccess(false)
            .permissions(Collections.emptySet())
            .build();

        assertNotEquals(role, new UserStateRole(REQUESTER.getId(), REQUESTER.toString(), false, Collections.emptySet(), Collections.emptySet()));
    }

}
