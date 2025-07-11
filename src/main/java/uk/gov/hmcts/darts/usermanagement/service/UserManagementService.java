package uk.gov.hmcts.darts.usermanagement.service;

import uk.gov.hmcts.darts.usermanagement.model.User;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;
import uk.gov.hmcts.darts.usermanagement.model.UserSearch;
import uk.gov.hmcts.darts.usermanagement.model.UserWithId;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndTimestamps;

import java.util.List;

public interface UserManagementService {

    UserWithIdAndTimestamps modifyUser(Integer userId, UserPatch userPatch);

    UserWithId createUser(User user);

    List<UserWithIdAndTimestamps> search(UserSearch userSearch);

    List<UserWithIdAndTimestamps> getUsers(boolean includeSystemUsers, String emailAddress, List<Integer> userIds);

    UserWithIdAndTimestamps getUserById(Integer userId);

}
