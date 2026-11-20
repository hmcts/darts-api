package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.darts.arm.service.impl.CleanUpDetsDataProcessorImpl;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ObjectStateRecordRepository extends JpaRepository<ObjectStateRecordEntity, Long> {
    Optional<ObjectStateRecordEntity> findByArmEodId(long armEodId);

    // The timeout is transaction-local so it cannot leak into later work on the pooled connection.
    @Query(value = "SELECT set_config('statement_timeout', :timeout, true)", nativeQuery = true)
    String setCleanupStatementTimeout(@Param("timeout") String timeout);

    
    @Query(value = "SELECT osr_uuid AS osrUuid, dets_location AS detsLocation  " +
        "FROM darts.dets_cleanup_eod_osr_rows(:limit, :last_modified_before_ts)", nativeQuery = true)
    List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse> cleanUpDetsDataProcedure(
        @Param("limit") Integer limit, @Param("last_modified_before_ts") OffsetDateTime lastModifiedBefore);

}
