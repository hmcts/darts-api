package uk.gov.hmcts.darts.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.EventType;

import java.util.List;

@Repository
public interface EventTypeRepository extends JpaRepository<EventType, Integer> {

    List<EventType> findByHandler(String handlerName);
}
