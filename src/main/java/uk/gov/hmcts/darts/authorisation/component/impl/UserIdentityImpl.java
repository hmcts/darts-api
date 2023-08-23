package uk.gov.hmcts.darts.authorisation.component.impl;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;

import java.util.List;

@Component
public class UserIdentityImpl implements UserIdentity {

    @Override
    public String getEmailAddress() {
        Object principalObject = SecurityContextHolder.getContext()
            .getAuthentication()
            .getPrincipal();

        if (principalObject instanceof Jwt jwt) {
            Object emailsAddressesObject = jwt.getClaims().get("emails");

            if (emailsAddressesObject instanceof List<?> emails) {
                if (emails.size() != 1) {
                    throw new IllegalStateException(String.format("Unexpected number of email addresses: %d", emails.size()));
                }
                Object emailAddressObject = emails.get(0);

                if (emailAddressObject instanceof String emailAddress) {
                    if (StringUtils.hasText(emailAddress)) {
                        return emailAddress;
                    }
                }
            }
        }

        throw new IllegalStateException("Could not obtain email address from principal");
    }

}
