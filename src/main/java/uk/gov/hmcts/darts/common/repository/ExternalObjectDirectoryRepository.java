package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ExternalObjectDirectoryRepository extends JpaRepository<ExternalObjectDirectoryEntity, Integer> {

    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod, MediaEntity med
            WHERE eod.media = :media AND eod.status = :status AND eod.externalLocationType = :externalLocationType
            """
    )
    List<ExternalObjectDirectoryEntity> findByMediaStatusAndType(MediaEntity media, ObjectDirectoryStatusEntity status,
                                                                 ExternalLocationTypeEntity externalLocationType);

    @Query("SELECT eod FROM ExternalObjectDirectoryEntity eod WHERE eod.externalLocationType = :externalLocationTypeEntity AND eod.status = :status")
    List<ExternalObjectDirectoryEntity> findByExternalLocationTypeAndMarkedForDeletion(ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                                       ObjectDirectoryStatusEntity status);
    List<ExternalObjectDirectoryEntity> findByMediaAndExternalLocationType(MediaEntity media,
                                                                           ExternalLocationTypeEntity externalLocationType);

    @Query(
        """
            SELECT eod.id FROM ExternalObjectDirectoryEntity eod, ExternalObjectDirectoryEntity eod2
            WHERE eod.media is not null
            AND eod.media = eod2.media
            AND eod.status = :status1
            AND eod2.status = :status2
            AND eod.externalLocationType = :location1
            AND eod2.externalLocationType = :location2
            AND eod2.lastModifiedDateTime < :lastModifiedBefore
            """
    )
    List<Integer> findMediaFileIdsIn2StorageLocationsBeforeTime(ObjectDirectoryStatusEntity status1,
                                                                ObjectDirectoryStatusEntity status2,
                                                                ExternalLocationTypeEntity location1,
                                                                ExternalLocationTypeEntity location2,
                                                                OffsetDateTime lastModifiedBefore);

    @Modifying(clearAutomatically = true)
    @Query(
        """
            update ExternalObjectDirectoryEntity eod
            set eod.status = :newStatus,
            eod.lastModifiedBy = :userAccount,
            eod.lastModifiedDateTime = :timestamp
            where eod.id in :idsToDelete
            """)
    void updateStatus(ObjectDirectoryStatusEntity newStatus, UserAccountEntity userAccount, List<Integer> idsToDelete, OffsetDateTime timestamp);

}
