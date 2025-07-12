package uk.gov.hmcts.darts.usermanagement.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.validation.IdRequest;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotSameUserValidatorTest {

    @Mock
    private AuthorisationApi authorisationApi;

    private NotSameUserValidator notSameUserValidator;

    @BeforeEach
    void setUp() {
        notSameUserValidator = new NotSameUserValidator(authorisationApi);
    }

    @Test
    void validate_ShouldThrowException_WhenUserIsSelf() {

        Integer userId = 101;

        Set<UserAccountEntity> userEntitySet = new HashSet<>();
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setId(userId);
        userEntitySet.add(userAccountEntity);

        when(authorisationApi.getCurrentUser()).thenReturn(userAccountEntity);

        UserPatch patch = new UserPatch();
        patch.setActive(false);
        IdRequest<UserPatch, Integer> request = new IdRequest<>(patch, userId);

        DartsApiException exception = Assertions.assertThrows(DartsApiException.class, () ->
            notSameUserValidator.validate(request));

        Assertions.assertEquals(AuthorisationError.UNABLE_TO_DEACTIVATE_USER.getTitle(), exception.getMessage());
    }

    @Test
    void validate_ShouldReturn_WhenUserIsNotCurrentUser() {

        Set<UserAccountEntity> userEntitySet = new HashSet<>();
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setId(102);
        userEntitySet.add(userAccountEntity);

        when(authorisationApi.getCurrentUser()).thenReturn(userAccountEntity);
        Integer userId = 101;
        UserPatch patch = new UserPatch();
        patch.setActive(false);
        IdRequest<UserPatch, Integer> request = new IdRequest<>(patch, userId);

        Assertions.assertDoesNotThrow(() -> notSameUserValidator.validate(request));
    }
}