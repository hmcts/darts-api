package uk.gov.hmcts.darts.usermanagement.service.impl;

import uk.gov.hmcts.darts.usermanagement.model.User;

public interface UserCreationValidation {

    void validate(User validatable);

}
