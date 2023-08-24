package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAccountStub {
    private final UserAccountRepository userAccountRepository;

    public UserAccountEntity getDefaultUser() {
        Optional<UserAccountEntity> found = userAccountRepository.findById(1);
        return found.orElseGet(this::createTestUserAccountEntity);
    }

    private UserAccountEntity createTestUserAccountEntity() {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setUsername("Test account");
        userAccount.setLastModifiedBy(userAccount);
        userAccount.setCreatedBy(userAccount);
        userAccountRepository.saveAndFlush(userAccount);
        return userAccount;
    }

}
