package uk.gov.hmcts.darts.usermanagement.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.usermanagement.component.UserManagementQuery;

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.jpa.domain.Specification.where;
import static uk.gov.hmcts.darts.usermanagement.component.impl.UserQuerySpecifications.hasEmailAddress;
import static uk.gov.hmcts.darts.usermanagement.component.impl.UserQuerySpecifications.isInIds;
import static uk.gov.hmcts.darts.usermanagement.component.impl.UserQuerySpecifications.notSystemUser;

@Component
@RequiredArgsConstructor
public class UserManagementQueryImpl implements UserManagementQuery {

    private final UserAccountRepository userAccountRepository;

    @Override
    public List<UserAccountEntity> getUsers(String emailAddress, List<Integer> userIds) {
        return userAccountRepository.findAll(
            where(notSystemUser())
                .and(hasEmailAddress(emailAddress))
                .and(isInIds(userIds)),
            Sort.by(DESC, "id"));
    }
}
