package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.SecurityPermissionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.CPP;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DAR_PC;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.HMCTS_TRANSCRIPTION_HUB;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.MID_TIER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.XHIBIT;

class SecurityRoleRepositoryTest extends IntegrationBase {

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

    @ParameterizedTest(name = "{0} should have no permissions")
    @MethodSource("rolesWithNoPermissions")
    void shouldFindNoPermissions(SecurityRoleEnum securityRole) {
        SecurityRoleEntity securityRoleEntity = securityRoleRepository.findById(securityRole.getId()).orElseThrow();
        final Set<SecurityPermissionEntity> securityPermissionEntities = securityRoleEntity.getSecurityPermissionEntities();
        assertTrue(securityPermissionEntities.isEmpty());
    }

    private static Stream<Arguments> rolesWithNoPermissions() {
        return Stream.of(
            Arguments.of(APPROVER),
            Arguments.of(REQUESTER),
            Arguments.of(JUDICIARY),
            Arguments.of(TRANSCRIBER),
            Arguments.of(TRANSLATION_QA),
            Arguments.of(RCJ_APPEALS),
            Arguments.of(XHIBIT),
            Arguments.of(CPP),
            Arguments.of(DAR_PC),
            Arguments.of(MID_TIER),
            Arguments.of(SUPER_ADMIN),
            Arguments.of(SUPER_USER),
            Arguments.of(HMCTS_TRANSCRIPTION_HUB)
        );
    }
}
