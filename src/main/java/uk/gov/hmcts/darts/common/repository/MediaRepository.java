package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, Integer> {

    @Query("""
           SELECT me
           FROM HearingEntity he
           JOIN he.mediaList me
           WHERE he.id = :hearingId
           ORDER BY me.start
        """)
    List<MediaEntity> findAllByHearingId(Integer hearingId);

}
