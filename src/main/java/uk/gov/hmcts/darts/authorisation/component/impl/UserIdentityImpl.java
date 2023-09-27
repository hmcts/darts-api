package uk.gov.hmcts.darts.authorisation.component.impl;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.List;
import java.util.Optional;

@Component
@AllArgsConstructor
public class UserIdentityImpl implements UserIdentity {

    private static final String EMAILS = "emails";
    private static final String PREFERRED_USERNAME = "preferred_username";

    private final UserAccountRepository userAccountRepository;

    @Override
    public String getEmailAddress() {
        Object principalObject = SecurityContextHolder.getContext()
            .getAuthentication()
            .getPrincipal();

        if (principalObject instanceof Jwt jwt) {
            Object emailsAddressesObject = jwt.getClaims().get(EMAILS);
            if (emailsAddressesObject == null) {
                emailsAddressesObject = jwt.getClaims().get(PREFERRED_USERNAME);
            }

            if (emailsAddressesObject instanceof List<?> emails) {
                if (emails.size() != 1) {
                    throw new IllegalStateException(String.format(
                        "Unexpected number of email addresses: %d",
                        emails.size()
                    ));
                }
                Object emailAddressObject = emails.get(0);

                if (emailAddressObject instanceof String emailAddress && StringUtils.isNotBlank(emailAddress)) {
                    return emailAddress;
                }
            } else if (emailsAddressesObject instanceof String emailAddress && StringUtils.isNotBlank(emailAddress)) {
                return emailAddress;
            }
        }

        throw new IllegalStateException("Could not obtain email address from principal");
    }

    public UserAccountEntity getUserAccount() {
        String emailAddress = getEmailAddress();
        Optional<UserAccountEntity> userAccount = userAccountRepository.findByEmailAddressIgnoreCase(emailAddress);
        if (userAccount.isEmpty()) {
            throw new IllegalStateException("Could not obtain user details");
        }
        return userAccount.get();
    }
}
