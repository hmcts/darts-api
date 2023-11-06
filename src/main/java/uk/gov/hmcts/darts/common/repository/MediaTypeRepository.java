package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.MediaTypeEntity;

@Repository
public interface MediaTypeRepository extends JpaRepository<MediaTypeEntity, Integer> {

}
