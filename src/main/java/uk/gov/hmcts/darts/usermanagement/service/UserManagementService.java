package uk.gov.hmcts.darts.usermanagement.service;

import uk.gov.hmcts.darts.usermanagement.model.User;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;
import uk.gov.hmcts.darts.usermanagement.model.UserWithId;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndLastLogin;

public interface UserManagementService {

    UserWithIdAndLastLogin modifyUser(Integer userId, UserPatch userPatch);

    UserWithId createUser(User user);

}
