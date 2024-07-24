package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;

import java.util.List;

@Repository
public interface EventHandlerRepository extends RevisionRepository<EventHandlerEntity, Integer, Long>, JpaRepository<EventHandlerEntity, Integer> {

    List<EventHandlerEntity> findByHandlerAndActiveTrue(String handlerName);

    @Query("""
        SELECT eh FROM EventHandlerEntity eh
        WHERE eh.type = :type
        AND (eh.subType is null or eh.subType = :subType)
        AND eh.active = true
        ORDER BY eh.subType desc nulls last
        """)
    List<EventHandlerEntity> findByTypeAndSubType(String type, String subType);

    @Query("""
        SELECT eh FROM EventHandlerEntity eh
        WHERE eh.type = :type
        AND ((:subType IS NULL AND eh.subType IS NULL) OR (:subType IS NOT NULL AND eh.subType = :subType))
        AND eh.active = true
        """)
    List<EventHandlerEntity> findActiveMappingsForTypeAndSubtype(String type, String subType);

    @Query(value = """
        SELECT * FROM darts.event_handler eh
        WHERE eh.created_by != 0
        """, nativeQuery = true)
    List<EventHandlerEntity> findByCreatedByIsNot0();

    List<EventHandlerEntity> findByIdGreaterThanEqual(Integer threshold);
}
