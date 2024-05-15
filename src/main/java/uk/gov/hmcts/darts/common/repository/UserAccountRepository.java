package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Integer>, JpaSpecificationExecutor<UserAccountEntity> {

    List<UserAccountEntity> findByEmailAddressIgnoreCase(String emailAddress);

    Optional<UserAccountEntity> findByAccountGuidAndActive(String guid, Boolean active);

    @Query("""
        SELECT DISTINCT userAccount
        FROM UserAccountEntity userAccount
        JOIN userAccount.securityGroupEntities securityGroup
        JOIN securityGroup.courthouseEntities courthouse
        JOIN securityGroup.securityRoleEntity securityRole
        WHERE courthouse = :courthouse
        AND securityRole.id = :securityRole
        AND userAccount.active = true
        """)
    List<UserAccountEntity> findByRoleAndCourthouse(int securityRole, CourthouseEntity courthouse);

    @Query("""
        SELECT userAccount
        FROM UserAccountEntity userAccount
        WHERE userAccount.active = true
        AND userAccount.isSystemUser = true
        AND userAccount.accountGuid = :uuid
        """)
    UserAccountEntity findSystemUser(String uuid);


    @Query("""
        SELECT DISTINCT userAccount
        FROM UserAccountEntity userAccount
        JOIN userAccount.securityGroupEntities securityGroup
        JOIN securityGroup.securityRoleEntity securityRole
        WHERE (upper(userAccount.emailAddress) = upper(:emailAddress) OR userAccount.accountGuid = :accountGuid)
        AND securityRole.id IN (:roleIds)
        AND securityGroup.globalAccess = true
        """)
    List<UserAccountEntity> findByEmailAddressOrAccountGuidForRolesAndGlobalAccessIsTrue(String emailAddress, String accountGuid, Set<Integer> roleIds);

    @Query("""
        SELECT DISTINCT userAccount
        FROM UserAccountEntity userAccount
        JOIN userAccount.securityGroupEntities securityGroup
        JOIN securityGroup.securityRoleEntity securityRole
        WHERE userAccount.id = :userId
        AND securityRole.id = :securityRoleId
        """)
    Optional<UserAccountEntity> findByRoleAndUserId(Integer securityRoleId, Integer userId);

    List<UserAccountEntity> findByEmailAddressIgnoreCaseAndActive(String emailAddress, Boolean active);

    @Modifying
    @Query("""
        UPDATE UserAccountEntity
        SET lastLoginTime = CURRENT_TIMESTAMP
        WHERE id = :userId
        """)
    void updateLastLoginTime(Integer userId);

    List<UserAccountEntity> findByIdIn(List<Integer> userIds);
}
