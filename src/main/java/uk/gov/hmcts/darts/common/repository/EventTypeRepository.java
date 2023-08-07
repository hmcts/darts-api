package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;

import java.util.List;

@Repository
public interface EventTypeRepository extends JpaRepository<EventHandlerEntity, Integer> {

    List<EventHandlerEntity> findByHandlerAndActiveTrue(String handlerName);
}
