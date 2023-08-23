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
        SELECT courthouse FROM SecurityGroupEntity securityGroup, UserAccountEntity userAccount
            JOIN securityGroup.courthouseEntities courthouse
            JOIN userAccount.securityGroupEntities
        WHERE userAccount.emailAddress = :emailAddress
        """)
    List<CourthouseEntity> findAuthorisedCourthousesForEmailAddress(String emailAddress);

}
