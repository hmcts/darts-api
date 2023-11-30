package uk.gov.hmcts.darts.usermanagement.service;

import uk.gov.hmcts.darts.usermanagement.model.User;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;
import uk.gov.hmcts.darts.usermanagement.model.UserSearch;
import uk.gov.hmcts.darts.usermanagement.model.UserWithId;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndLastLogin;

import java.util.List;

public interface UserManagementService {

    UserWithIdAndLastLogin modifyUser(Integer userId, UserPatch userPatch);

    UserWithId createUser(User user);

    List<UserWithIdAndLastLogin> search(UserSearch userSearch);

}
