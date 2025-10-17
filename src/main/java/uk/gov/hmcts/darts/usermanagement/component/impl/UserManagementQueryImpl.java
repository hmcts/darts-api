package uk.gov.hmcts.darts.usermanagement.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.component.UserManagementQuery;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserManagementQueryImpl implements UserManagementQuery {

    private final UserAccountRepository userAccountRepository;

    @Override
    public List<UserAccountEntity> getUsers(boolean includeSystemUsers, String emailAddress, List<Integer> userIds) {
        return userAccountRepository.findUsers(
            includeSystemUsers,
            emailAddress,
            userIds,
            Sort.by(Sort.Direction.DESC, "id")
        );
    }
}
