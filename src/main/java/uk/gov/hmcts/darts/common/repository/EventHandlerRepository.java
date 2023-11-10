package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventHandlerRepository extends JpaRepository<EventHandlerEntity, Integer> {

    List<EventHandlerEntity> findByHandlerAndActiveTrue(String handlerName);

    Optional<EventHandlerEntity> findByTypeAndSubTypeAndActiveTrue(String type, String subType);

    Optional<EventHandlerEntity> findByTypeAndSubTypeIsNullAndActiveTrue(String type);


}
