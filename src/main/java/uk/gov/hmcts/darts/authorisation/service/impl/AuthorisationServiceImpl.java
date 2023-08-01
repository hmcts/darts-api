package uk.gov.hmcts.darts.authorisation.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.model.Permission;
import uk.gov.hmcts.darts.authorisation.model.Role;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.authorisation.service.AuthorisationService;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity_;
import uk.gov.hmcts.darts.common.entity.SecurityPermissionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityPermissionEntity_;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity_;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity_;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthorisationServiceImpl implements AuthorisationService {

    @Autowired
    private EntityManager em;

    @Override
    public UserState getAuthorisation(String emailAddress) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<GetAuthorisationResult> criteriaQuery = criteriaBuilder.createQuery(GetAuthorisationResult.class);

        Root<UserAccountEntity> root = criteriaQuery.from(UserAccountEntity.class);
        Join<UserAccountEntity, SecurityGroupEntity> securityGroup = root.join(UserAccountEntity_.securityGroupEntities);
        Join<SecurityGroupEntity, SecurityRoleEntity> securityRole = securityGroup.join(
            SecurityGroupEntity_.securityRoleId);
        Join<SecurityRoleEntity, SecurityPermissionEntity> securityPermission = securityRole.join(
            SecurityRoleEntity_.securityPermissionEntities);

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

        TypedQuery<GetAuthorisationResult> query = em.createQuery(criteriaQuery);
        query.setParameter(paramEmailAddress, emailAddress);

        return getUserState(query.getResultList());
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private UserState getUserState(List<GetAuthorisationResult> getAuthorisationResultList) {
        UserState.UserStateBuilder userStateBuilder = UserState.builder();
        Integer tmpRoleId = 0;
        Set<Role> roles = new LinkedHashSet<>();
        Set<Permission> permissions = new LinkedHashSet<>();

        for (GetAuthorisationResult result : getAuthorisationResultList) {
            userStateBuilder.userId(result.userId());
            userStateBuilder.userName(result.userName());
            userStateBuilder.roles(roles);

            if (!tmpRoleId.equals(result.roleId())) {
                permissions = new LinkedHashSet<>();
                roles.add(Role.builder()
                              .roleId(result.roleId())
                              .roleName(result.roleName())
                              .permissions(permissions)
                              .build());

                tmpRoleId = result.roleId();
            }

            permissions.add(Permission.builder()
                                .permissionId(result.permissionId())
                                .permissionName(result.permissionName())
                                .build());
        }

        return userStateBuilder.build();
    }

}
