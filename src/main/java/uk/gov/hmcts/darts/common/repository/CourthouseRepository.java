package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CourthouseRepository extends JpaRepository<CourthouseEntity, Integer> {

    Optional<CourthouseEntity> findByCode(int code);

    Optional<CourthouseEntity> findByCourthouseNameIgnoreCase(String name);

    @Query("""
        SELECT DISTINCT courthouse
        FROM UserAccountEntity userAccount
        JOIN userAccount.securityGroupEntities securityGroup
        JOIN securityGroup.courthouseEntities courthouse
        JOIN securityGroup.securityRoleEntity securityRole
        WHERE (lower(userAccount.emailAddress) = lower(:emailAddress) or
        userAccount.accountGuid = :guid)
        AND securityRole.id IN (:roleIds)
        """)
    List<CourthouseEntity> findAuthorisedCourthousesForEmailAddressOrGuid(String emailAddress, Set<Integer> roleIds, String guid);

    @Query("""
        SELECT ch.id
        FROM CourthouseEntity ch
        WHERE upper(ch.courthouseName) like upper(CONCAT('%', :name, '%'))
        or upper(ch.displayName) like upper(CONCAT('%', :name, '%'))
        """)
    List<Integer> findAllIdByDisplayNameOrNameLike(String name);

    Optional<CourthouseEntity> findByDisplayNameIgnoreCase(String displayName);

    boolean existsByCourthouseNameIgnoreCaseAndIdNot(String name, Integer id);

    boolean existsByDisplayNameIgnoreCaseAndIdNot(String name, Integer id);

    List<CourthouseEntity> findByIdIn(List<Integer> courthouseIds);

}
