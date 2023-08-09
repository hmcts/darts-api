package uk.gov.hmcts.darts.authorisation.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.authorisation.service.AuthorisationService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthorisationApiImpl implements AuthorisationApi {

    private final AuthorisationService authorisationService;

    @Override
    public Optional<UserState> getAuthorisation(String emailAddress) {
        return authorisationService.getAuthorisation(emailAddress);
    }

}
