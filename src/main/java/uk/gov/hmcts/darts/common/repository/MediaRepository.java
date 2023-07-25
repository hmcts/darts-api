package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, Integer> {

    @Query(
        value = "SELECT med.* FROM darts.media med "
              + "INNER JOIN darts.hearing_media_ae hma on med.med_id = hma.med_id "
              + "INNER JOIN darts.hearing hea on hea.hea_id = hma.hea_id "
              + "WHERE hma.hea_id = :hearingId", nativeQuery = true)
    List<MediaEntity> findAllByHearingId(Integer hearingId);

}

