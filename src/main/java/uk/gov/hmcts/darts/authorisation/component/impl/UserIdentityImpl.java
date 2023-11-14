package uk.gov.hmcts.darts.authorisation.component.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.USER_DETAILS_INVALID;

@Component
@AllArgsConstructor
@Slf4j
public class UserIdentityImpl implements UserIdentity {

    private static final String EMAILS = "emails";
    private static final String PREFERRED_USERNAME = "preferred_username";
    private static final String OID = "oid";

    private final UserAccountRepository userAccountRepository;

    public String getEmailAddressFromToken() {
        if (nonNull(SecurityContextHolder.getContext().getAuthentication())) {
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
        }
        throw new IllegalStateException("Could not obtain email address from principal");
    }

    public String getGuidFromToken() {
        if (nonNull(SecurityContextHolder.getContext().getAuthentication())) {
            Object principalObject = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

            Object oid = null;
            if (principalObject instanceof Jwt jwt) {
                oid = jwt.getClaims().get(OID);
            }
            if (nonNull(oid) && oid instanceof String guid && StringUtils.isNotBlank(guid)) {
                return guid;
            }
        }
        return null;
    }

    public UserAccountEntity getUserAccount() {
        UserAccountEntity userAccount = null;
        String guid = getGuidFromToken();
        if (nonNull(guid)) {
            // System users will use guid not email address
            userAccount = userAccountRepository.findByAccountGuid(guid).orElse(null);
        }
        if (isNull(userAccount)) {
            userAccount = userAccountRepository.findByEmailAddressIgnoreCase(getEmailAddressFromToken())
                .orElseThrow(() -> new DartsApiException(USER_DETAILS_INVALID));
        }
        return userAccount;
    }

    public boolean userHasGlobalAccess(Set<SecurityRoleEnum> globalAccessRoles) {
        boolean userHasGlobalAccess = false;
        String emailAddress = null;
        String guid = getGuidFromToken();

        try {
            emailAddress = getEmailAddressFromToken();
        } catch (IllegalStateException e) {
            log.debug("Unable to get email address from token: {}", e.getMessage());
        }

        if (nonNull(guid) || nonNull(emailAddress)) {
            List<UserAccountEntity> userAccountEntities =
                userAccountRepository.findByEmailAddressOrAccountGuidForRolesAndGlobalAccessIsTrue(
                    emailAddress, guid,
                    globalAccessRoles.stream().map(SecurityRoleEnum::getId).collect(Collectors.toUnmodifiableSet())
                );
            if (!userAccountEntities.isEmpty()) {
                userHasGlobalAccess = true;
            }
        }
        return userHasGlobalAccess;
    }
}
