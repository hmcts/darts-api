package uk.gov.hmcts.darts.courthouse;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.repository.NoAuthorisation;
import uk.gov.hmcts.darts.common.repository.RestrictedAccessRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourthouseRepository extends RestrictedAccessRepository<CourthouseEntity, Integer> {

    @Query("SELECT courthouse FROM SecurityGroupEntity securityGroup, UserAccountEntity userAccount"
        + JPA_SECURITY_FRAGMENT)
    List<CourthouseEntity> findAll();

    @Query(
        "SELECT courthouse FROM CourthouseEntity courthouseEntity, SecurityGroupEntity securityGroup, UserAccountEntity userAccount"
            + JPA_SECURITY_FRAGMENT +
            "AND courthouseEntity.code = :code")
    Optional<CourthouseEntity> findByCode(int code);

    @Query(
        "SELECT courthouse FROM CourthouseEntity courthouseEntity, SecurityGroupEntity securityGroup, UserAccountEntity userAccount"
            + JPA_SECURITY_FRAGMENT
            + "AND upper(courthouseEntity.courthouseName) = upper(:name)")
    Optional<CourthouseEntity> findByCourthouseNameIgnoreCase(String name);

    @NoAuthorisation
    CourthouseEntity save(CourthouseEntity entity);

    @NoAuthorisation
    void deleteById(Integer id);

    @NoAuthorisation
    void deleteAll();

    @NoAuthorisation
    CourthouseEntity saveAndFlush(CourthouseEntity entity);

    @NoAuthorisation
    CourthouseEntity getReferenceById(Integer integer);

}
