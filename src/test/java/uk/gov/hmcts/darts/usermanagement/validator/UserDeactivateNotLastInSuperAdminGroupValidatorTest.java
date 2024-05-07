package uk.gov.hmcts.darts.usermanagement.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.validation.UserQueryRequest;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class UserDeactivateNotLastInSuperAdminGroupValidatorTest {

    private UserDeactivateNotLastInSuperAdminGroupValidator userDeactivateNotLastSuperAdminValidator;

    SecurityGroupRepository repository;

    public UserDeactivateNotLastInSuperAdminGroupValidatorTest() {
        repository = Mockito.mock(SecurityGroupRepository.class);
        userDeactivateNotLastSuperAdminValidator = new UserDeactivateNotLastInSuperAdminGroupValidator(repository);
    }

    @Test
    public void validateFail() throws Exception {
        SecurityGroupEntity securityGroupEntity = Mockito.mock(SecurityGroupEntity.class);
        Mockito.when(repository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_ADMIN.getName())).thenReturn(Optional.of(securityGroupEntity));

        Integer userId = 100;

        Set<UserAccountEntity> userEntitySet = new HashSet<>();
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setId(userId);
        userEntitySet.add(userAccountEntity);

        Mockito.when(securityGroupEntity.getUsers()).thenReturn(userEntitySet);

        UserPatch patch = new UserPatch();
        patch.setActive(false);
        UserQueryRequest<UserPatch> request = new UserQueryRequest<>(patch, userId);

        DartsApiException ex = Assertions.assertThrows(DartsApiException.class, () -> userDeactivateNotLastSuperAdminValidator.validate(request));
        Assertions.assertEquals(AuthorisationError.USER_NOT_AUTHORISED_FOR_ENDPOINT.getTitle(), ex.getMessage());
    }

    @Test
    public void validateSuccessUserNotLast() throws Exception {
        SecurityGroupEntity securityGroupEntity = Mockito.mock(SecurityGroupEntity.class);
        Mockito.when(repository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_ADMIN.getName())).thenReturn(Optional.of(securityGroupEntity));

        Integer userId = 100;

        Set<UserAccountEntity> userEntitySet = new HashSet<>();
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setId(101);
        userEntitySet.add(userAccountEntity);

        Mockito.when(securityGroupEntity.getUsers()).thenReturn(userEntitySet);

        UserPatch patch = new UserPatch();
        patch.setActive(false);
        UserQueryRequest<UserPatch> request = new UserQueryRequest<>(patch, userId);

        userDeactivateNotLastSuperAdminValidator.validate(request);
    }

    @Test
    public void validateSuccessActive() throws Exception {
        SecurityGroupEntity securityGroupEntity = Mockito.mock(SecurityGroupEntity.class);
        Mockito.when(repository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_ADMIN.getName())).thenReturn(Optional.of(securityGroupEntity));

        Integer userId = 100;

        Set<UserAccountEntity> userEntitySet = new HashSet<>();
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setId(101);
        userEntitySet.add(userAccountEntity);

        Mockito.when(securityGroupEntity.getUsers()).thenReturn(userEntitySet);

        UserPatch patch = new UserPatch();
        patch.setActive(true);
        UserQueryRequest<UserPatch> request = new UserQueryRequest<>(patch, userId);

        userDeactivateNotLastSuperAdminValidator.validate(request);
    }
}