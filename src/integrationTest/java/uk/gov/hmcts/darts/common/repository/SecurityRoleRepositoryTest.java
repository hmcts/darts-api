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
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.CPP;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DAR_PC;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.MID_TIER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.XHIBIT;

@Transactional
class SecurityRoleRepositoryTest extends IntegrationBase {

    @Autowired
    private SecurityRoleRepository securityRoleRepository;

    @Test
    void shouldFindAllSecurityRoles() {
        List<SecurityRoleEntity> securityRoleEntityList = securityRoleRepository.findAll();
        assertEquals(12, securityRoleEntityList.size());
    }

    @Test
    void shouldFindAllApproverPermissions() {
        SecurityRoleEntity approverRole = securityRoleRepository.findById(APPROVER.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = approverRole.getSecurityPermissionEntities();
        assertEquals(11, securityPermissionEntities.size());
    }

    @Test
    void shouldFindAllRequesterPermissions() {
        SecurityRoleEntity requesterRole = securityRoleRepository.findById(REQUESTER.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = requesterRole.getSecurityPermissionEntities();
        assertEquals(10, securityPermissionEntities.size());
    }

    @Test
    void shouldFindAllJudgePermissions() {
        SecurityRoleEntity judgeRole = securityRoleRepository.findById(JUDGE.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = judgeRole.getSecurityPermissionEntities();
        assertEquals(12, securityPermissionEntities.size());
    }

    @Test
    void shouldFindAllTranscriberPermissions() {
        SecurityRoleEntity transcriberRole = securityRoleRepository.findById(TRANSCRIBER.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = transcriberRole.getSecurityPermissionEntities();
        assertEquals(12, securityPermissionEntities.size());
    }

    @Test
    void shouldFindAllTranslationQaPermissions() {
        SecurityRoleEntity translationQaRole = securityRoleRepository.findById(TRANSLATION_QA.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = translationQaRole.getSecurityPermissionEntities();
        assertEquals(7, securityPermissionEntities.size());
    }

    @Test
    void shouldFindAllRcjAppealsPermissions() {
        SecurityRoleEntity rcjAppealsRole = securityRoleRepository.findById(RCJ_APPEALS.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = rcjAppealsRole.getSecurityPermissionEntities();
        assertEquals(7, securityPermissionEntities.size());
    }

    @Test
    void shouldFindAllXhibitPermissions() {
        SecurityRoleEntity xhibitRole = securityRoleRepository.findById(XHIBIT.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = xhibitRole.getSecurityPermissionEntities();
        assertEquals(1, securityPermissionEntities.size());
    }

    @Test
    void shouldFindAllCppPermissions() {
        SecurityRoleEntity cppRole = securityRoleRepository.findById(CPP.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = cppRole.getSecurityPermissionEntities();
        assertEquals(1, securityPermissionEntities.size());
    }

    @Test
    void shouldFindAllDarPcPermissions() {
        SecurityRoleEntity darPcRole = securityRoleRepository.findById(DAR_PC.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = darPcRole.getSecurityPermissionEntities();
        assertEquals(2, securityPermissionEntities.size());
    }

    @Test
    void shouldFindAllMidTierPermissions() {
        SecurityRoleEntity midTierRole = securityRoleRepository.findById(MID_TIER.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = midTierRole.getSecurityPermissionEntities();
        assertEquals(4, securityPermissionEntities.size());
    }

    @Test
    void shouldFindAllSuperAdminPermissions() {
        SecurityRoleEntity superAdminRole = securityRoleRepository.findById(SUPER_ADMIN.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = superAdminRole.getSecurityPermissionEntities();
        assertEquals(15, securityPermissionEntities.size());
    }

    @Test
    void shouldFindAllSuperUserPermissions() {
        SecurityRoleEntity superUserRole = securityRoleRepository.findById(SUPER_USER.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = superUserRole.getSecurityPermissionEntities();
        assertEquals(13, securityPermissionEntities.size());
    }

}
