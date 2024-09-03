package uk.gov.hmcts.darts.annotation.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@ExtendWith(MockitoExtension.class)
class UserAuthorisedToDeleteAnnotationValidatorTest {
    @Mock
    private AnnotationRepository annotationRepository;

    @Mock
    private AuthorisationApi authorisationApi;

    private UserAuthorisedToDeleteAnnotationValidator userAuthorisedToDeleteAnnotationValidator;

    @BeforeEach
    void setUp() {
        userAuthorisedToDeleteAnnotationValidator = new UserAuthorisedToDeleteAnnotationValidator(annotationRepository, authorisationApi);
    }

    @Test
    void doesntThrowIfUserIsSuperAdmin() {
        when(annotationRepository.findById(1)).thenReturn(Optional.of(new AnnotationEntity()));
        when(authorisationApi.getCurrentUser()).thenReturn(userWithRole(SUPER_ADMIN));
        when(authorisationApi.userHasOneOfRoles(List.of(SUPER_ADMIN))).thenReturn(true);

        assertThatNoException().isThrownBy(() -> userAuthorisedToDeleteAnnotationValidator.validate(1));
    }

    @Test
    void doesntThrowIfUserIsJudgeAndCreatedTheAnnotation() {
        var judge = userWithRole(JUDICIARY);
        when(authorisationApi.getCurrentUser()).thenReturn(judge);
        when(annotationRepository.findById(1)).thenReturn(Optional.of(annotationCreatedBy(judge)));

        assertThatNoException().isThrownBy(() -> userAuthorisedToDeleteAnnotationValidator.validate(1));
    }

    private static AnnotationEntity annotationCreatedBy(UserAccountEntity judge) {
        var annotationEntity = new AnnotationEntity();
        annotationEntity.setId(1);
        annotationEntity.setCurrentOwner(judge);
        return annotationEntity;
    }

    private UserAccountEntity userWithRole(SecurityRoleEnum role) {
        var userAccountEntity = new UserAccountEntity();
        userAccountEntity.setId(1);
        userAccountEntity.getSecurityGroupEntities().add(securityGroupWithRole(role));
        return userAccountEntity;
    }

    private SecurityGroupEntity securityGroupWithRole(SecurityRoleEnum role) {
        var securityGroupEntity = new SecurityGroupEntity();
        securityGroupEntity.setSecurityRoleEntity(securityRoleOf(role));
        securityGroupEntity.setDisplayState(true);
        return securityGroupEntity;
    }

    private SecurityRoleEntity securityRoleOf(SecurityRoleEnum role) {
        var securityRoleEntity = new SecurityRoleEntity();
        securityRoleEntity.setRoleName(role.name());
        securityRoleEntity.setDisplayState(true);
        return securityRoleEntity;
    }
}
