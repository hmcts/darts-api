package uk.gov.hmcts.darts.usermanagement.component;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

public interface UserSearchQuery {

    List<UserAccountEntity> getUsers(String fullName, String emailAddress);

}
