package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, Integer> {

    @Query(
        "SELECT med FROM HearingMediaEntity hma, MediaEntity med " +
            "WHERE hma.hearing.id = :hearingId " +
            "AND med = hma.media "
    )
    List<MediaEntity> findAllByHearingId(Integer hearingId);

}

