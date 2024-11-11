package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;

import java.util.Optional;

import static uk.gov.hmcts.darts.common.entity.CourtroomEntity.TABLE_NAME;

@Repository
public interface CourtroomRepository extends JpaRepository<CourtroomEntity, Integer> {

    @Query("""
        SELECT cr FROM CourthouseEntity ch, CourtroomEntity cr
        WHERE ch.courthouseName = upper(:courthouse)
        AND cr.name = upper(:courtroom)
        AND cr.courthouse = ch
        """
    )
    Optional<CourtroomEntity> findByCourthouseNameAndCourtroomName(String courthouse, String courtroom);

    @Query(value = "SELECT * FROM {h-schema}" + TABLE_NAME + " cr " +
        "WHERE cr." + CourtroomEntity.COURTROOM_NAME + " = upper(:courtroom) " +
        "AND cr." + CourtroomEntity.CTH_ID + " = :courthouseId ", nativeQuery = true
    )
    Optional<CourtroomEntity> findByNameAndId(int courthouseId, String courtroom);

}
