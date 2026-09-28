package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.AudioIngestionEntity;

import java.time.OffsetDateTime;
import java.util.List;


@Repository
public interface AudioIngestionRepository extends JpaRepository<AudioIngestionEntity, Long> {
    /**
     * Returns the number of items currently eligible for ingestion.
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM darts.audio_ingestion
        WHERE status = 'NEW'
           OR (
               status = 'PROCESSING'
               AND claimed_at < :processingCutoff
           )
        """, nativeQuery = true)
    long countEligibleForProcessing(OffsetDateTime processingCutOff);

    /**
     * Atomically claims a maximum of five eligible items.
     */
    @Transactional
    @Query(value = """
        UPDATE darts.audio_ingestion
        SET status = 'PROCESSING',
            claim_owner = :claimOwner,
            claimed_at = CURRENT_TIMESTAMP
        WHERE id IN (
            SELECT id
            FROM darts.audio_ingestion
            WHERE status = 'NEW'
               OR (
                   status = 'PROCESSING'
                   AND claimed_at < :processingCutOff
               )
            ORDER BY source_last_modified_ts, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
        )
        RETURNING id
        """, nativeQuery = true)
    List<Long> claimForProcessing(String claimOwner, OffsetDateTime processingCutOff, int batchSize);

    /**
     * Marks an item as permanently failed.
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE darts.audio_ingestion
        SET status = 'FAILED',
            error = :error
        WHERE id = :id
          AND status = 'PROCESSING'
          AND claim_owner = :claimOwner
        """, nativeQuery = true)
    int markAsFailed(
        Long id,
        String claimOwner,
        String error
    );
}