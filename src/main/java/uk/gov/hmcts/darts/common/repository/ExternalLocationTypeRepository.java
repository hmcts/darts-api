package uk.gov.hmcts.darts.common.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;

import java.util.Optional;

@Repository
public interface ExternalLocationTypeRepository extends JpaRepository<ExternalLocationTypeEntity, Integer> {

    @Cacheable("externalLocationTypeEntity")
    @Override
    ExternalLocationTypeEntity getReferenceById(Integer id);

    @Cacheable("externalLocationTypeEntityOptional")
    @Override
    Optional<ExternalLocationTypeEntity> findById(Integer id);
}
