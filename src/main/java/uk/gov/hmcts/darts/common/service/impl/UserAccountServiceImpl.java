package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.UserAccountService;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountRepository userAccountRepository;

    @Transactional
    @Override
    public void updateLastLoginTime(Integer userId) {
        var now = OffsetDateTime.now(ZoneId.of("UTC"));
        userAccountRepository.updateLastLoginTime(userId, now);
    }

}
