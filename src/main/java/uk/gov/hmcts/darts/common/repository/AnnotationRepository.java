package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

@Repository
public interface AnnotationRepository extends JpaRepository<AnnotationEntity, Integer> {

    @Query("""
        SELECT ann
        FROM AnnotationEntity ann
         JOIN ann.annotationDocuments annDoc
         JOIN ann.hearingList hearing
        WHERE hearing.id = :hearingId
        AND ann.deleted = false
        """)
    List<AnnotationEntity> findByHearingId(int hearingId);

    @Query("""
        SELECT ann
        FROM AnnotationEntity ann
         JOIN ann.annotationDocuments annDoc
         JOIN ann.hearingList hearing
        WHERE hearing.id = :hearingId
        AND ann.deleted = false
        AND ann.currentOwner = :userAccount
        """)
    List<AnnotationEntity> findByHearingIdAndUser(int hearingId, UserAccountEntity userAccount);

    @Query("""
        SELECT ann
        FROM AnnotationEntity ann
         JOIN ann.annotationDocuments annDoc
         JOIN ann.hearingList hearing
        WHERE hearing.id in :hearingIds
        AND ann.deleted = false
        ORDER By hearing.hearingDate DESC        
        """)
    List<AnnotationEntity> findByListOfHearingIds(List<Integer> hearingIds);

    @Query("""
        SELECT ann
        FROM AnnotationEntity ann
         JOIN ann.annotationDocuments annDoc
         JOIN ann.hearingList hearing
        WHERE hearing.id in :hearingIds
        AND ann.deleted = false
        AND ann.currentOwner = :userAccount
        ORDER By hearing.hearingDate DESC        
        """)
    List<AnnotationEntity> findByListOfHearingIdsAndUser(List<Integer> hearingIds, UserAccountEntity userAccount);

    @Modifying
    @Query("update AnnotationEntity ann set ann.deleted = true, ann.lastModifiedBy = :userAccount where ann.id = :id")
    void markForDeletion(@Param("id") Integer id, @Param("userAccount") UserAccountEntity userAccount);
}
