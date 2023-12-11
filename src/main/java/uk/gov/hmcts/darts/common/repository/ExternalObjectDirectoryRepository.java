package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ExternalObjectDirectoryRepository extends JpaRepository<ExternalObjectDirectoryEntity, Integer> {

    @Query(
        "SELECT eod FROM ExternalObjectDirectoryEntity eod " +
            "WHERE eod.media = :media AND eod.status = :status AND eod.externalLocationType = :externalLocationType"
    )
    List<ExternalObjectDirectoryEntity> findByMediaStatusAndType(MediaEntity media, ObjectRecordStatusEntity status,
                                                                 ExternalLocationTypeEntity externalLocationType);

    @Query(
        "SELECT eod FROM ExternalObjectDirectoryEntity eod " +
            "WHERE eod.status = :status AND eod.externalLocationType = :type"
    )
    List<ExternalObjectDirectoryEntity> findByStatusAndType(ObjectRecordStatusEntity status, ExternalLocationTypeEntity type);

    @Query(
        "SELECT eod FROM ExternalObjectDirectoryEntity eod " +
            "WHERE (eod.status = :status1 " +
            "OR eod.status = :status2 " +
            "OR eod.status = :status3 " +
            "OR eod.status = :status4 " +
            "OR eod.status = :status5 " +
            "OR eod.status = :status6) " +
            "AND eod.externalLocationType = :type"
    )
    List<ExternalObjectDirectoryEntity> findByFailedAndType(ObjectRecordStatusEntity status1,
                                                            ObjectRecordStatusEntity status2,
                                                            ObjectRecordStatusEntity status3,
                                                            ObjectRecordStatusEntity status4,
                                                            ObjectRecordStatusEntity status5,
                                                            ObjectRecordStatusEntity status6,
                                                            ExternalLocationTypeEntity type);

    @Query("SELECT eod FROM ExternalObjectDirectoryEntity eod WHERE eod.externalLocationType = :externalLocationTypeEntity AND eod.status = :status")
    List<ExternalObjectDirectoryEntity> findByExternalLocationTypeAndMarkedForDeletion(ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                                       ObjectRecordStatusEntity status);

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
    List<Integer> findMediaFileIdsIn2StorageLocationsBeforeTime(ObjectRecordStatusEntity status1,
                                                                ObjectRecordStatusEntity status2,
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
    void updateStatus(ObjectRecordStatusEntity newStatus, UserAccountEntity userAccount, List<Integer> idsToDelete, OffsetDateTime timestamp);

}
