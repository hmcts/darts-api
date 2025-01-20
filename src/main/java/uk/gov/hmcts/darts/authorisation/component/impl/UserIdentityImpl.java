package uk.gov.hmcts.darts.authorisation.component.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.component.DartsJwt;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.util.EmailAddressFromTokenUtil;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.repository.UserRolesCourthousesRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.USER_DETAILS_INVALID;

@Component
@AllArgsConstructor
@Slf4j
public class UserIdentityImpl implements UserIdentity {
    private static final String OID = "oid";

    private final UserAccountRepository userAccountRepository;
    private final UserRolesCourthousesRepository userRolesCourthousesRepository;

    private String getGuidFromToken(Jwt token) {
        if (token != null) {
            Object oid = token.getClaims().get(OID);
            if (nonNull(oid) && oid instanceof String guid && StringUtils.isNotBlank(guid)) {
                return guid;
            }
        }
        return null;
    }

    @Override
    public UserAccountEntity getUserAccount() {
        return getUserAccount(getJwt());
    }

    @Override
    public UserAccountEntity getUserAccount(Jwt jwt) {
        return getUserAccountOptional(jwt)
            .orElseThrow(() -> new DartsApiException(USER_DETAILS_INVALID));
    }

    @Override
    public Optional<UserAccountEntity> getUserAccountOptional(Jwt jwt) {
        String guid = getGuidFromToken(jwt);

        return Optional.ofNullable(guid)
            .map(guidValue -> userAccountRepository.findByAccountGuidAndActive(guidValue, true))
            .orElseGet(() -> {
                String emailAddressFromToken = EmailAddressFromTokenUtil.getEmailAddressFromToken(jwt);
                return userAccountRepository.findByEmailAddressIgnoreCaseAndActive(emailAddressFromToken, true)
                    .stream()
                    .findFirst();
            });
    }

    @Override
    public Jwt getJwt() {
        if (SecurityContextHolder.getContext().getAuthentication() != null
            && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }

    @Override
    public boolean userHasGlobalAccess(Set<SecurityRoleEnum> globalAccessRoles) {
        boolean userHasGlobalAccess = false;
        String emailAddress = null;
        Jwt jwt = getJwt();
        String guid = getGuidFromToken(jwt);

        try {
            if (jwt != null) {
                emailAddress = EmailAddressFromTokenUtil.getEmailAddressFromToken(jwt);
            }
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

    @Override
    public Optional<Integer> getUserIdFromJwt() {
        if (getJwt() instanceof DartsJwt dartsJwt) {
            return Optional.ofNullable(dartsJwt.getUserId());
        }
        return Optional.empty();
    }
}