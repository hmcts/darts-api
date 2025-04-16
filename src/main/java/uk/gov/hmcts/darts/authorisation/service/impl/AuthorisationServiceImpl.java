package uk.gov.hmcts.darts.authorisation.service.impl;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.jpa.SpecHints;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.authorisation.model.UserStateRole;
import uk.gov.hmcts.darts.authorisation.service.AuthorisationService;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity_;
import uk.gov.hmcts.darts.common.entity.SecurityPermissionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity_;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity_;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.CouplingBetweenObjects")//TODO - refactor to reduce coupling when this class is next edited
public class AuthorisationServiceImpl implements AuthorisationService {

    private final CourthouseRepository courthouseRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserIdentity userIdentity;
    private final EntityManager entityManager;

    private EntityGraph<UserAccountEntity> userAccountEntityEntityGraph;

    /**
     * Obtain UserState, querying by UserAccount.id.
     *
     * <p>A corresponding UserAccount is expected to exist. A NoResultException will be thrown otherwise.
     *
     * @param userId the user's id
     * @return the UserState corresponding to the provided user id.
     */
    @Override
    public UserState getAuthorisation(int userId) {
        UserAccountEntity userById = entityManager.createQuery(
                """
                    SELECT user FROM UserAccountEntity user
                    WHERE user.id = :id
                    """,
                UserAccountEntity.class)
            .setParameter("id", userId)
            .setHint(SpecHints.HINT_SPEC_FETCH_GRAPH, getUserAccountEntityGraph())
            .getSingleResult();

        return getUserState(userById);
    }

    /**
     * Obtain UserState, querying by UserAccount.emailAddress.
     *
     * @param emailAddress the user's email address
     * @return an optional UserState corresponding to the provided email address (if the account exists), or an empty Optional if no matching account exists.
     */
    @Override
    public Optional<UserState> getAuthorisation(String emailAddress) {
        UserAccountEntity userById;
        try {
            userById = entityManager.createQuery(
                    """
                        SELECT user FROM UserAccountEntity user
                        WHERE UPPER(user.emailAddress) = UPPER(:emailAddress)
                        AND user.active = true
                        """,
                    UserAccountEntity.class)
                .setParameter("emailAddress", emailAddress)
                .setHint(SpecHints.HINT_SPEC_FETCH_GRAPH, getUserAccountEntityGraph())
                .getSingleResult();

        } catch (NoResultException e) {
            return Optional.empty();
        }

        return Optional.of(getUserState(userById));
    }

    /**
     * Build an entity graph encompassing all security groups, courthouses, roles and permissions related to a user.
     */
    private EntityGraph<UserAccountEntity> getUserAccountEntityGraph() {
        if (userAccountEntityEntityGraph == null) {
            var entityGraph = entityManager.createEntityGraph(UserAccountEntity.class);
            entityGraph.addAttributeNodes(UserAccountEntity_.SECURITY_GROUP_ENTITIES);

            var securityGroupSubgraph = entityGraph.addSubgraph(UserAccountEntity_.SECURITY_GROUP_ENTITIES);
            securityGroupSubgraph.addAttributeNodes(SecurityGroupEntity_.SECURITY_ROLE_ENTITY,
                                                    SecurityGroupEntity_.COURTHOUSE_ENTITIES);

            var securityRoleSubgraph = securityGroupSubgraph.addSubgraph(SecurityGroupEntity_.SECURITY_ROLE_ENTITY);
            securityRoleSubgraph.addAttributeNodes(SecurityRoleEntity_.SECURITY_PERMISSION_ENTITIES);

            userAccountEntityEntityGraph = entityGraph;
        }

        return userAccountEntityEntityGraph;
    }

    private UserState getUserState(UserAccountEntity userAccountEntity) {
        return UserState.builder()
            .userId(userAccountEntity.getId())
            .userName(userAccountEntity.getUserFullName())
            .isActive(userAccountEntity.isActive())
            .roles(mapRoles(userAccountEntity.getSecurityGroupEntities()))
            .build();
    }

    private Set<UserStateRole> mapRoles(Set<SecurityGroupEntity> securityGroupEntities) {
        Map<SecurityRoleEntity, List<SecurityGroupEntity>> securityGroupsByRole = securityGroupEntities.stream()
            .collect(Collectors.groupingBy(SecurityGroupEntity::getSecurityRoleEntity));

        Set<UserStateRole> userStateRoles = new HashSet<>();
        for (Map.Entry<SecurityRoleEntity, List<SecurityGroupEntity>> entrySet : securityGroupsByRole.entrySet()) {
            final SecurityRoleEntity roleEntity = entrySet.getKey();
            final List<SecurityGroupEntity> securityGroupEntitiesForRole = entrySet.getValue();

            UserStateRole userStateRole = UserStateRole.builder()
                .roleId(roleEntity.getId())
                .roleName(roleEntity.getRoleName())
                .globalAccess(hasAnyGroupHaveGlobalAccess(securityGroupEntitiesForRole))
                .courthouseIds(getAssociatedCourthouses(securityGroupEntitiesForRole))
                .permissions(getAssociatedPermissions(roleEntity))
                .build();
            userStateRoles.add(userStateRole);
        }

        return userStateRoles;
    }

    private boolean hasAnyGroupHaveGlobalAccess(List<SecurityGroupEntity> securityGroupEntities) {
        return securityGroupEntities.stream()
            .anyMatch(SecurityGroupEntity::getGlobalAccess);
    }

    private Set<Integer> getAssociatedCourthouses(List<SecurityGroupEntity> securityGroupEntities) {
        return securityGroupEntities.stream()
            .map(SecurityGroupEntity::getCourthouseEntities)
            .flatMap(Collection::stream)
            .map(CourthouseEntity::getId)
            .collect(Collectors.toSet());
    }

    private Set<String> getAssociatedPermissions(SecurityRoleEntity roleEntity) {
        return roleEntity.getSecurityPermissionEntities().stream()
            .map(SecurityPermissionEntity::getPermissionName)
            .collect(Collectors.toSet());
    }

    @Override
    public void checkCourthouseAuthorisation(List<CourthouseEntity> courthouses, Set<SecurityRoleEnum> securityRoles) {
        if (courthouses.isEmpty()) {
            throw new DartsApiException(AuthorisationError.USER_NOT_AUTHORISED_FOR_COURTHOUSE);
        }
        UserAccountEntity userAccount = userIdentity.getUserAccount();
        if (nonNull(userAccount)) {
            String emailAddress = userAccount.getEmailAddress();
            List<CourthouseEntity> authorisedCourthouses = courthouseRepository.findAuthorisedCourthousesForEmailAddressOrGuid(
                emailAddress,
                securityRoles.stream().map(SecurityRoleEnum::getId).collect(Collectors.toUnmodifiableSet()),
                userAccount.getAccountGuid()
            );

            if (new HashSet<>(authorisedCourthouses).containsAll(courthouses)) {
                return;
            }
            log.debug("User {} is not authorised for courthouses {}, securityRoles {}", emailAddress,
                      courthouses.stream().map(CourthouseEntity::getCourthouseName).toList(),
                      securityRoles
            );
            throw new DartsApiException(AuthorisationError.USER_NOT_AUTHORISED_FOR_COURTHOUSE);
        }
        throw new DartsApiException(AuthorisationError.USER_DETAILS_INVALID);
    }

    @Override
    public List<UserAccountEntity> getUsersWithRoleAtCourthouse(SecurityRoleEnum securityRole,
                                                                CourthouseEntity courthouse,
                                                                UserAccountEntity... excludingUsers) {
        return userAccountRepository.findByRoleAndCourthouse(securityRole.getId(), courthouse, Set.of(excludingUsers));
    }

}