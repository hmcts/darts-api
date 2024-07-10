package uk.gov.hmcts.darts.authorisation.controller.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.controller.AuthorisationController;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Optional;

@Slf4j
@RestController
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
@RequiredArgsConstructor
public class AuthorisationControllerImpl implements AuthorisationController {

    private final AuthorisationApi authorisationApi;

    @Override
    public UserState getUserState() {
        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        Optional<UserState> userStateOptional = authorisationApi.getAuthorisation(currentUser.getEmailAddress());
        if (userStateOptional.isEmpty()) {
            throw new DartsApiException(AuthorisationError.USER_NOT_AUTHORISED_FOR_ENDPOINT);
        }
        return userStateOptional.get();
    }
}
