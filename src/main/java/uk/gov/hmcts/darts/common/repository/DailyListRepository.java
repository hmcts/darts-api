package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("PMD.MethodNamingConventions")
@Repository
public interface DailyListRepository extends JpaRepository<DailyListEntity, Integer> {
    Optional<DailyListEntity> findByUniqueId(String uniqueId);

    List<DailyListEntity> findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDescCreatedDateTimeDesc(
        String listingCourthouse, JobStatusType status, LocalDate date, String source);

    List<DailyListEntity> findByStatusAndStartDateAndSourceOrderByPublishedTimestampDescCreatedDateTimeDesc(
        JobStatusType status, LocalDate date, String source);

    List<DailyListEntity> deleteByStartDateBefore(LocalDate startDate, Limit limit);
}
