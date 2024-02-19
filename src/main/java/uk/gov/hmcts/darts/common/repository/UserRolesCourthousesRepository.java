package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.UserRolesCourthousesEntity;
import uk.gov.hmcts.darts.common.repository.base.ReadOnlyRepository;

import java.util.List;

@Repository
public interface UserRolesCourthousesRepository extends ReadOnlyRepository<UserRolesCourthousesEntity, Integer> {

    @Query("""
        SELECT DISTINCT urc.courthouse.id
        FROM UserRolesCourthousesEntity urc
        WHERE urc.userAccount.active = true
        AND urc.userAccount = :userAccount
        """)
    List<Integer> findAllCourthouseIdsByUserAccount(UserAccountEntity userAccount);

}
