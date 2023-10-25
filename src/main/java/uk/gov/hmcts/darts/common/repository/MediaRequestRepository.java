package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface MediaRequestRepository extends JpaRepository<MediaRequestEntity, Integer> {

    Optional<MediaRequestEntity> findTopByStatusOrderByCreatedDateTimeAsc(AudioRequestStatus status);

    long countByRequestor_IdAndLastAccessedDateTime(Integer id, OffsetDateTime lastAccessedDateTime);

}
