package uk.gov.hmcts.darts.authorisation.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static java.util.Objects.nonNull;

@UtilityClass
public class EmailAddressFromTokenUtil {
    private static final String EMAIL = "email";
    private static final String EMAILS = "emails";
    private static final String PREFERRED_USERNAME = "preferred_username";
    private static final List<String> CLAIM_ORDER = List.of(EMAILS, EMAIL, PREFERRED_USERNAME);

    public String getEmailAddressFromToken() {
        Jwt jwt = getJwtFromSecurityContext();
        Object claimFromJwt = getClaimFromJwt(jwt, CLAIM_ORDER);
        String emailAddressFromObject = getEmailAddressFromObject(claimFromJwt);
        if (emailAddressFromObject == null) {
            throw new IllegalStateException("Could not obtain email address from principal");
        }
        return emailAddressFromObject;
    }

    /*
    Pass in a list of claims to check in the order you want to check them.
     */
    private Object getClaimFromJwt(Jwt jwt, List<String> claimNamesToCheck) {
        for (String claimToCheck : claimNamesToCheck) {
            Object claimObject = jwt.getClaims().get(claimToCheck);
            if (claimObject != null) {
                return claimObject;
            }
        }
        return null;
    }

    private String getEmailAddressFromObject(Object emailsAddressesObject) {
        if (emailsAddressesObject instanceof List<?> emails) {
            if (emails.size() != 1) {
                throw new IllegalStateException(String.format(
                    "Unexpected number of email addresses: %d",
                    emails.size()
                ));
            }
            Object emailAddressObject = emails.getFirst();

            if (emailAddressObject instanceof String emailAddress && StringUtils.isNotBlank(emailAddress)) {
                return emailAddress;
            }
        } else if (emailsAddressesObject instanceof String emailAddress && StringUtils.isNotBlank(emailAddress)) {
            return emailAddress;
        }
        return null;
    }

    private Jwt getJwtFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (nonNull(authentication)) {
            Object principalObject = authentication.getPrincipal();

            if (principalObject instanceof Jwt jwt) {
                return jwt;
            }
        }
        throw new IllegalStateException("Could not obtain email address from principal");
    }
}
