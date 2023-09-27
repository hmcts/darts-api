package uk.gov.hmcts.darts.audio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;

import java.util.List;

@Repository
public interface MediaRequestRepository extends JpaRepository<MediaRequestEntity, Integer> {

    List<MediaRequestEntity> findByStatusOrderByCreatedDateTimeAsc(AudioRequestStatus status);

}
