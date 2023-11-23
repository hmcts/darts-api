package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Integer> {

    Optional<UserAccountEntity> findByEmailAddressIgnoreCase(String emailAddress);

    Optional<UserAccountEntity> findByAccountGuid(String guid);

    Optional<UserAccountEntity> findByAccountGuidAndIsSystemUserTrue(String guid);

    //todo find out what user states are Active
    @Query("""
        SELECT DISTINCT userAccount
        FROM UserAccountEntity userAccount
        JOIN userAccount.securityGroupEntities securityGroup
        JOIN securityGroup.courthouseEntities courthouse
        JOIN securityGroup.securityRoleEntity securityRole
        WHERE courthouse = :courthouse
        AND securityRole.id = :securityRole
        AND userAccount.state = 1
        """)
    List<UserAccountEntity> findByRoleAndCourthouse(int securityRole, CourthouseEntity courthouse);

    @Query("""
        SELECT userAccount
        FROM UserAccountEntity userAccount
        WHERE userAccount.state = 1
        AND userAccount.isSystemUser = true
        AND userAccount.accountGuid = :uuid
        """)
    UserAccountEntity findSystemUser(String uuid);


    @Query("""
        SELECT DISTINCT userAccount
        FROM UserAccountEntity userAccount
        JOIN userAccount.securityGroupEntities securityGroup
        JOIN securityGroup.securityRoleEntity securityRole
        WHERE (userAccount.emailAddress = :emailAddress OR userAccount.accountGuid = :accountGuid)
        AND securityRole.id IN (:roleIds)
        AND securityGroup.globalAccess = true
        """)
    List<UserAccountEntity> findByEmailAddressOrAccountGuidForRolesAndGlobalAccessIsTrue(String emailAddress, String accountGuid, Set<Integer> roleIds);

}
