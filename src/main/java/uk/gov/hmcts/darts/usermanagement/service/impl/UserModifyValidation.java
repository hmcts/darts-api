package uk.gov.hmcts.darts.usermanagement.service.impl;

import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

public interface UserModifyValidation {

    void validate(UserPatch validatable, Integer userId);

}
