package uk.gov.hmcts.darts.authorisation.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.authorisation.model.GetAuthorisationResult;
import uk.gov.hmcts.darts.authorisation.model.Permission;
import uk.gov.hmcts.darts.authorisation.model.Role;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.authorisation.service.AuthorisationService;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity_;
import uk.gov.hmcts.darts.common.entity.SecurityPermissionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityPermissionEntity_;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity_;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity_;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthorisationServiceImpl implements AuthorisationService {

    private final EntityManager em;
    private final CourthouseRepository courthouseRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserIdentity userIdentity;

    @Override
    public Optional<UserState> getAuthorisation(String emailAddress) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<GetAuthorisationResult> criteriaQuery = criteriaBuilder.createQuery(GetAuthorisationResult.class);

        Root<UserAccountEntity> root = criteriaQuery.from(UserAccountEntity.class);
        Join<UserAccountEntity, SecurityGroupEntity> securityGroup = root.join(
            UserAccountEntity_.securityGroupEntities,
            JoinType.LEFT
        );
        Join<SecurityGroupEntity, SecurityRoleEntity> securityRole = securityGroup.join(
            SecurityGroupEntity_.securityRoleEntity,
            JoinType.LEFT
        );
        Join<SecurityRoleEntity, SecurityPermissionEntity> securityPermission = securityRole.join(
            SecurityRoleEntity_.securityPermissionEntities,
            JoinType.LEFT
        );

        criteriaQuery.select(criteriaBuilder.construct(
                                 GetAuthorisationResult.class,
                                 root.get(UserAccountEntity_.id),
                                 root.get(UserAccountEntity_.username),
                                 securityRole.get(SecurityRoleEntity_.id),
                                 securityRole.get(SecurityRoleEntity_.roleName),
                                 securityPermission.get(SecurityPermissionEntity_.id),
                                 securityPermission.get(SecurityPermissionEntity_.permissionName)
                             )
        );

        ParameterExpression<String> paramEmailAddress = criteriaBuilder.parameter(String.class);
        criteriaQuery.where(criteriaBuilder.equal(
            criteriaBuilder.lower(root.get(UserAccountEntity_.emailAddress)),
            criteriaBuilder.lower(paramEmailAddress)
        ));
        criteriaQuery.orderBy(List.of(
            criteriaBuilder.asc(securityRole.get(SecurityRoleEntity_.id)),
            criteriaBuilder.asc(securityPermission.get(SecurityPermissionEntity_.id))
        ));

        TypedQuery<GetAuthorisationResult> query = em.createQuery(criteriaQuery);
        query.setParameter(paramEmailAddress, emailAddress);

        return getUserState(query.getResultList());
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private Optional<UserState> getUserState(List<GetAuthorisationResult> getAuthorisationResultList) {
        if (getAuthorisationResultList.isEmpty()) {
            return Optional.empty();
        }

        UserState.UserStateBuilder userStateBuilder = UserState.builder();
        Set<Role> roles = new LinkedHashSet<>();
        userStateBuilder.roles(roles);

        Integer tmpRoleId = 0;
        Set<Permission> permissions = new LinkedHashSet<>();
        for (GetAuthorisationResult result : getAuthorisationResultList) {
            userStateBuilder.userId(result.userId());
            userStateBuilder.userName(result.userName());

            Integer roleId = result.roleId();
            if (roleId != null && !tmpRoleId.equals(roleId)) {
                permissions = new LinkedHashSet<>();
                roles.add(Role.builder()
                              .roleId(roleId)
                              .roleName(result.roleName())
                              .permissions(permissions)
                              .build());

                tmpRoleId = roleId;
            }

            Integer permissionId = result.permissionId();
            if (permissionId != null) {
                permissions.add(Permission.builder()
                                    .permissionId(permissionId)
                                    .permissionName(result.permissionName())
                                    .build());
            }
        }

        return Optional.ofNullable(userStateBuilder.build());
    }

    @Override
    public void checkCourthouseAuthorisation(List<CourthouseEntity> courthouses, Set<SecurityRoleEnum> securityRoles) {
        UserAccountEntity userAccount = userIdentity.getUserAccount();
        if (nonNull(userAccount)) {
            String emailAddress = userAccount.getEmailAddress();
            List<CourthouseEntity> authorisedCourthouses = courthouseRepository.findAuthorisedCourthousesForEmailAddress(
                emailAddress,
                securityRoles.stream().map(SecurityRoleEnum::getId).collect(Collectors.toUnmodifiableSet())
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
                                                                CourthouseEntity courthouse) {
        return userAccountRepository.findByRoleAndCourthouse(securityRole.getId(), courthouse);
    }

}
