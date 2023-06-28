package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;

import static uk.gov.hmcts.darts.common.entity.CourtroomEntity.TABLE_NAME;

@Repository
public interface CourtroomRepository extends JpaRepository<CourtroomEntity, Integer> {

    @Query("SELECT cr FROM CourthouseEntity ch, CourtroomEntity cr " +
        "WHERE upper(ch.courthouseName) = upper(:courthouse) " +
        "AND upper(cr.name) = upper(:courtroom) " +
        "AND cr.courthouse = ch.id "
    )
    CourtroomEntity findByNames(String courthouse, String courtroom);

    @Query(value = "SELECT * FROM {h-schema}" + TABLE_NAME + " cr " +
        "WHERE upper(cr." + CourtroomEntity.COURTROOM_NAME + ") = upper(:courtroom) " +
        "AND cr." + CourtroomEntity.MOJ_CTH_ID + " = :courthouseId ", nativeQuery = true
    )
    CourtroomEntity findByNameAndId(int courthouseId, String courtroom);

}
