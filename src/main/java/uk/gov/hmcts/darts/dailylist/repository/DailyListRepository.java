package uk.gov.hmcts.darts.dailylist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyListRepository extends JpaRepository<DailyListEntity, Integer> {
    List<DailyListEntity> findById(int id);

    Optional<DailyListEntity> findByUniqueId(String uniqueId);

    List<DailyListEntity> findByStatusIn(List<String> status);

}
