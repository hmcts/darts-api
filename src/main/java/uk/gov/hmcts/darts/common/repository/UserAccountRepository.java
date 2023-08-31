package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Integer> {

    Optional<UserAccountEntity> findByEmailAddressIgnoreCase(String emailAddress);

}
