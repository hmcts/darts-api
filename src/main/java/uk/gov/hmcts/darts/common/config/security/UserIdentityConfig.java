package uk.gov.hmcts.darts.common.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.component.impl.AtsUserIdentityImpl;
import uk.gov.hmcts.darts.authorisation.component.impl.UserIdentityImpl;
import uk.gov.hmcts.darts.common.config.mode.ConditionOnAts;
import uk.gov.hmcts.darts.common.config.mode.ConditionOnNotAts;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.repository.UserRolesCourthousesRepository;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class UserIdentityConfig {
    @Bean
    @Conditional(ConditionOnAts.class)
    public UserIdentity getAtsIdentity(SystemUserHelper systemUserHelper,
                                       UserAccountRepository userAccountRepository, UserRolesCourthousesRepository userRolesCourthousesRepository) {
        return new AtsUserIdentityImpl(systemUserHelper, userAccountRepository, userRolesCourthousesRepository);
    }

    @Bean
    @Conditional(ConditionOnNotAts.class)
    public UserIdentity getIdentity(UserAccountRepository userAccountRepository,
                                    UserRolesCourthousesRepository userRolesCourthousesRepository) {
        return new UserIdentityImpl(userAccountRepository, userRolesCourthousesRepository);
    }
}