package uk.gov.hmcts.darts.courthouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourthouseRepository extends JpaRepository<CourthouseEntity, Integer> {

    Optional<CourthouseEntity> findByCode(int code);

    Optional<CourthouseEntity> findByCourthouseNameIgnoreCase(String name);

    @Query("""
        SELECT DISTINCT courthouse
        FROM UserAccountEntity userAccount
        JOIN userAccount.securityGroupEntities securityGroup
        JOIN securityGroup.courthouseEntities courthouse
        WHERE lower(userAccount.emailAddress) = lower(:emailAddress)
        """)
    List<CourthouseEntity> findAuthorisedCourthousesForEmailAddress(String emailAddress);

}
