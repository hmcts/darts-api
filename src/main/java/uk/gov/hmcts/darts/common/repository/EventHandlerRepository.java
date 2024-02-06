package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;

import java.util.List;

@Repository
public interface EventHandlerRepository extends JpaRepository<EventHandlerEntity, Integer> {

    List<EventHandlerEntity> findByHandlerAndActiveTrue(String handlerName);

    @Query("""
          SELECT eh FROM EventHandlerEntity eh
          WHERE eh.type = :type
          AND (eh.subType is null or eh.subType = :subType)
          AND eh.active = true
          ORDER BY eh.subType desc nulls last
          """)
    List<EventHandlerEntity> findByTypeAndSubType(String type, String subType);


}
