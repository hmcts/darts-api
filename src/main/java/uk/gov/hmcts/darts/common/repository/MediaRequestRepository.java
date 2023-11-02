package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRequestRepository extends JpaRepository<MediaRequestEntity, Integer> {

    Optional<MediaRequestEntity> findTopByStatusOrderByCreatedDateTimeAsc(AudioRequestStatus status);

    long countByRequestor_IdAndStatusAndLastAccessedDateTime(Integer id, AudioRequestStatus status, OffsetDateTime lastAccessedDateTime);


    @Query("SELECT m.id FROM MediaRequestEntity m WHERE lastAccessedDateTime < :lastAccessedDateTime AND status = :status")
    List<Integer> findAllIdsByLastAccessedTimeBeforeAndStatus(OffsetDateTime lastAccessedDateTime, AudioRequestStatus status);

    @Query("SELECT m.id FROM MediaRequestEntity m WHERE createdDateTime < :createdDateTime AND status <> :status AND lastAccessedDateTime IS NULL")
    List<Integer> findAllByCreatedDateTimeBeforeAndStatusNotAndLastAccessedDateTimeIsNull(OffsetDateTime createdDateTime, AudioRequestStatus status);

}
