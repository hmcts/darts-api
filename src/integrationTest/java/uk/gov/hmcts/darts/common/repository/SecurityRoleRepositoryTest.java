package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.SecurityPermissionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;

@Transactional
class SecurityRoleRepositoryTest extends IntegrationBase {

    @Autowired
    private SecurityRoleRepository securityRoleRepository;

    @Test
    void shouldFindAllSecurityRoles() {
        List<SecurityRoleEntity> securityRoleEntityList = securityRoleRepository.findAll();
        assertEquals(11, securityRoleEntityList.size());
    }

    @Test
    void shouldFindAllApproverPermissions() {
        SecurityRoleEntity courtManagerRole = securityRoleRepository.findById(APPROVER.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = courtManagerRole.getSecurityPermissionEntities();
        assertEquals(11, securityPermissionEntities.size());
    }

    @Test
    void shouldFindAllJudgePermissions() {
        SecurityRoleEntity judgeRole = securityRoleRepository.findById(JUDGE.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = judgeRole.getSecurityPermissionEntities();
        assertEquals(12, securityPermissionEntities.size());
    }

}
