package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Integer> {

    Optional<UserAccountEntity> findByEmailAddressIgnoreCase(String emailAddress);

    //todo find out what user states are valid
    @Query("""
        SELECT DISTINCT userAccount
        FROM UserAccountEntity userAccount
        JOIN userAccount.securityGroupEntities securityGroup
        JOIN securityGroup.courthouseEntities courthouse
        JOIN securityGroup.securityRoleEntity securityRole
        WHERE courthouse in (:courthouses)
        AND securityRole.id = :securityRole
        AND userAccount.state = 1
        """)
    List<UserAccountEntity> findByRoleAndCourthouses(int securityRole, List<CourthouseEntity> courthouses);


}
