package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransientObjectDirectoryRepository extends JpaRepository<TransientObjectDirectoryEntity, Integer> {

    @SuppressWarnings("PMD.MethodNamingConventions")
    Optional<TransientObjectDirectoryEntity> getTransientObjectDirectoryEntityByMediaRequest_Id(Integer mediaRequestId);

    List<TransientObjectDirectoryEntity> findByMediaRequest_idIn(List<Integer> mediaRequestIds);

    List<TransientObjectDirectoryEntity> findByStatus(ObjectDirectoryStatusEntity status);

}
