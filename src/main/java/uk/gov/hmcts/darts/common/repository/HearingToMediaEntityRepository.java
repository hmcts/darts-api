package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.HearingToMediaEntity;

@Repository
public interface HearingToMediaEntityRepository extends JpaRepository<HearingToMediaEntity, HearingToMediaEntity> {

    @Modifying
    @Query("DELETE FROM HearingToMediaEntity h WHERE h.media.id = :id")
    void deleteAllByMedia(Integer id);
}
