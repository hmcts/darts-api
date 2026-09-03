package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.SecurityPermissionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.CPP;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DAR_PC;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIAL_CONDUCT;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.MID_TIER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.XHIBIT;

class SecurityRoleRepositoryTest extends PostgresIntegrationBase {

    @Autowired
    private SecurityRoleRepository securityRoleRepository;

    @BeforeEach
    void startHibernateSession() {
        openInViewUtil.openEntityManager();
    }

    @AfterEach
    void closeHibernateSession() {
        openInViewUtil.closeEntityManager();
    }

    @Test
    void shouldFindAllSecurityRoles() {
        List<SecurityRoleEntity> securityRoleEntityList = securityRoleRepository.findAll();
        assertEquals(16, securityRoleEntityList.size());
    }

    @Test
    void shouldFindAllApproverPermissions() {
        SecurityRoleEntity approverRole = securityRoleRepository.findById(APPROVER.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = approverRole.getSecurityPermissionEntities();
        assertFalse(securityPermissionEntities.isEmpty());
    }

    @Test
    void shouldFindAllRequesterPermissions() {
        SecurityRoleEntity requesterRole = securityRoleRepository.findById(REQUESTER.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = requesterRole.getSecurityPermissionEntities();
        assertFalse(securityPermissionEntities.isEmpty());
    }

    @Test
    void shouldFindAllJudgePermissions() {
        SecurityRoleEntity judgeRole = securityRoleRepository.findById(JUDICIARY.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = judgeRole.getSecurityPermissionEntities();
        assertFalse(securityPermissionEntities.isEmpty());
    }

    @Test
    void shouldFindAllTranscriberPermissions() {
        SecurityRoleEntity transcriberRole = securityRoleRepository.findById(TRANSCRIBER.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = transcriberRole.getSecurityPermissionEntities();
        assertFalse(securityPermissionEntities.isEmpty());
    }

    @Test
    void shouldFindAllTranslationQaPermissions() {
        SecurityRoleEntity translationQaRole = securityRoleRepository.findById(TRANSLATION_QA.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = translationQaRole.getSecurityPermissionEntities();
        assertFalse(securityPermissionEntities.isEmpty());
    }

    @Test
    void shouldFindAllRcjAppealsPermissions() {
        SecurityRoleEntity rcjAppealsRole = securityRoleRepository.findById(RCJ_APPEALS.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = rcjAppealsRole.getSecurityPermissionEntities();
        assertFalse(securityPermissionEntities.isEmpty());
    }

    @Test
    void shouldFindAllXhibitPermissions() {
        SecurityRoleEntity xhibitRole = securityRoleRepository.findById(XHIBIT.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = xhibitRole.getSecurityPermissionEntities();
        assertFalse(securityPermissionEntities.isEmpty());
    }

    @Test
    void shouldFindAllCppPermissions() {
        SecurityRoleEntity cppRole = securityRoleRepository.findById(CPP.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = cppRole.getSecurityPermissionEntities();
        assertFalse(securityPermissionEntities.isEmpty());
    }

    @Test
    void shouldFindAllDarPcPermissions() {
        SecurityRoleEntity darPcRole = securityRoleRepository.findById(DAR_PC.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = darPcRole.getSecurityPermissionEntities();
        assertFalse(securityPermissionEntities.isEmpty());
    }

    @Test
    void shouldFindAllMidTierPermissions() {
        SecurityRoleEntity midTierRole = securityRoleRepository.findById(MID_TIER.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = midTierRole.getSecurityPermissionEntities();
        assertFalse(securityPermissionEntities.isEmpty());
    }

    @Test
    void shouldFindAllSuperAdminPermissions() {
        SecurityRoleEntity superAdminRole = securityRoleRepository.findById(SUPER_ADMIN.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = superAdminRole.getSecurityPermissionEntities();
        assertFalse(securityPermissionEntities.isEmpty());
    }

    @Test
    void shouldFindAllSuperUserPermissions() {
        SecurityRoleEntity superUserRole = securityRoleRepository.findById(SUPER_USER.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = superUserRole.getSecurityPermissionEntities();
        assertFalse(securityPermissionEntities.isEmpty());
    }

    @Test
    void shouldFindAllJudicialConductPermissions() {
        SecurityRoleEntity superUserRole = securityRoleRepository.findById(JUDICIAL_CONDUCT.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = superUserRole.getSecurityPermissionEntities();
        //TODO this will be false once the judicial conduct role is filled out
        assertTrue(securityPermissionEntities.isEmpty());
    }
    
}
