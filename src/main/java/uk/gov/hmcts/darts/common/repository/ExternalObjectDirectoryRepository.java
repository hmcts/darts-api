package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;

import java.util.List;

@Repository
public interface ExternalObjectDirectoryRepository extends JpaRepository<ExternalObjectDirectoryEntity, Integer> {

    @Query(
        "SELECT eod FROM ExternalObjectDirectoryEntity eod, MediaEntity med " +
            "WHERE eod.media = :media AND eod.status = :status AND eod.externalLocationType = :externalLocationType"
    )
    List<ExternalObjectDirectoryEntity> findByMediaStatusAndType(MediaEntity media, ObjectDirectoryStatusEntity status,
                                                                 ExternalLocationTypeEntity externalLocationType);

    @Query(
        "SELECT eod FROM ExternalObjectDirectoryEntity eod " +
            "WHERE eod.status = :status AND eod.externalLocationType = :type"
    )
    List<ExternalObjectDirectoryEntity> findByStatusAndType(ObjectDirectoryStatusEntity status, ExternalLocationTypeEntity type);

    @Query(
        "SELECT eod FROM ExternalObjectDirectoryEntity eod " +
            "WHERE eod.status != :status AND eod.externalLocationType = :type"
    )
    List<ExternalObjectDirectoryEntity> findByNotStatusAndType(ObjectDirectoryStatusEntity status, ExternalLocationTypeEntity type);

}
