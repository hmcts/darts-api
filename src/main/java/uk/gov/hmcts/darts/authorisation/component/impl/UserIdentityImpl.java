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
import uk.gov.hmcts.darts.common.repository.UserRolesCourthousesRepository;

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
    private final UserRolesCourthousesRepository userRolesCourthousesRepository;

    @SuppressWarnings({"PMD.AvoidDeeplyNestedIfStmts", "PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    private String getEmailAddressFromToken(Jwt token) {
        Object principalObject = token;

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

        return "";
    }

    private String getGuidFromToken(Jwt token) {
        Object principalObject = token;

        Object oid = null;
        if (principalObject instanceof Jwt jwt) {
            oid = jwt.getClaims().get(OID);
        }
        if (nonNull(oid) && oid instanceof String guid && StringUtils.isNotBlank(guid)) {
            return guid;
        }
        return null;
    }

    @Override
    public UserAccountEntity getUserAccount() {
        if (!(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("Could not obtain user from token");
        }

        return getUserAccount((Jwt)SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Override
    public UserAccountEntity getUserAccount(Jwt jwt) {
        UserAccountEntity userAccount = null;
        String guid = getGuidFromToken(jwt);
        if (nonNull(guid)) {
            // System users will use GUID not email address
            userAccount = userAccountRepository.findByAccountGuidAndActive(guid, true).orElse(null);
        }
        if (isNull(userAccount)) {
            userAccount = userAccountRepository.findByEmailAddressIgnoreCaseAndActive(getEmailAddressFromToken(jwt), true).stream()
                .findFirst()
                .orElseThrow(() -> new DartsApiException(USER_DETAILS_INVALID));
        }
        return userAccount;
    }

    private Jwt getJwt() {
        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof Jwt) {
            return (Jwt)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }
        return (Jwt)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Override
    public boolean userHasGlobalAccess(Set<SecurityRoleEnum> globalAccessRoles) {
        boolean userHasGlobalAccess = false;
        Jwt token = getJwt();
        String emailAddress = null;
        String guid = getGuidFromToken(token);

        try {
            emailAddress = getEmailAddressFromToken(token);
        } catch (IllegalStateException e) {
            if (nonNull(guid)) {
                log.debug("Guid is present but unable to get email address from token ending ''.....{}'': {}", StringUtils.right(guid, 5), e.getMessage());
            }
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
        } else {
            log.warn("Unable to get email address or guid from token");
        }
        return userHasGlobalAccess;
    }

    @Override
    public List<Integer> getListOfCourthouseIdsUserHasAccessTo() {
        UserAccountEntity userAccount = getUserAccount();
        return userRolesCourthousesRepository.findAllCourthouseIdsByUserAccount(userAccount);
    }
}