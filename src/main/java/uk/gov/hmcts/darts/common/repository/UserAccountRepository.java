package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserAccountRepository extends
    RevisionRepository<UserAccountEntity, Integer, Long>,
    JpaRepository<UserAccountEntity, Integer>,
    JpaSpecificationExecutor<UserAccountEntity> {

    Optional<UserAccountEntity> findFirstByEmailAddressIgnoreCase(String emailAddress);

    List<UserAccountEntity> findByIdGreaterThanEqual(Integer value);

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
        and userAccount not in :excludingUsers
        """)
    List<UserAccountEntity> findByRoleAndCourthouse(int securityRole, CourthouseEntity courthouse, Set<UserAccountEntity> excludingUsers);

    @Query("""
        SELECT DISTINCT userAccount
        FROM UserAccountEntity userAccount
        JOIN userAccount.securityGroupEntities securityGroup
        JOIN securityGroup.securityRoleEntity securityRole
        WHERE (upper(userAccount.emailAddress) = upper(:emailAddress) OR userAccount.accountGuid = :accountGuid)
        AND securityRole.id IN (:roleIds)
        AND securityGroup.globalAccess = true
        AND userAccount.active = true
        """)
    List<UserAccountEntity> findByEmailAddressOrAccountGuidForRolesAndGlobalAccessIsTrue(String emailAddress, String accountGuid, Set<Integer> roleIds);

    @Query("""
        SELECT DISTINCT userAccount
        FROM UserAccountEntity userAccount
        JOIN userAccount.securityGroupEntities securityGroup
        JOIN securityGroup.securityRoleEntity securityRole
        WHERE userAccount.id = :userId
        AND securityRole.id = :securityRoleId
        AND userAccount.active = true
        """)
    Optional<UserAccountEntity> findByRoleAndUserId(Integer securityRoleId, Integer userId);

    List<UserAccountEntity> findByEmailAddressIgnoreCase(String emailAddress);

    List<UserAccountEntity> findByEmailAddressIgnoreCaseAndActive(String emailAddress, Boolean active);

    List<UserAccountEntity> findByEmailAddressIgnoreCase(String emailAddress);

    @Modifying
    @Query("""
        UPDATE UserAccountEntity
        SET lastLoginTime = :now
        WHERE id = :userId
        """)
    void updateLastLoginTime(Integer userId, OffsetDateTime now);

    List<UserAccountEntity> findByIdInAndActive(List<Integer> userIds, Boolean active);
}
