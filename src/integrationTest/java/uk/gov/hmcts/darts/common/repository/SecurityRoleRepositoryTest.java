package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.SecurityPermissionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@Transactional
class SecurityRoleRepositoryTest {

    @Autowired
    private SecurityRoleRepository securityRoleRepository;

    @Test
    void shouldFindAllSecurityRoles() {
        List<SecurityRoleEntity> securityRoleEntityList = securityRoleRepository.findAll();
        assertEquals(10, securityRoleEntityList.size());
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
