package uk.gov.hmcts.darts.dailylist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;

import java.util.Optional;

@Repository
public interface DailyListRepository extends JpaRepository<DailyListEntity, Integer> {
    Optional<DailyListEntity> findByUniqueId(String uniqueId);
}
