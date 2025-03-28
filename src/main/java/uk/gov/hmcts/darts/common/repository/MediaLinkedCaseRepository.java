package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;

import java.util.List;

@Repository
public interface MediaLinkedCaseRepository extends JpaRepository<MediaLinkedCaseEntity, Integer> {

    List<MediaLinkedCaseEntity> findByMedia(MediaEntity media);

    boolean existsByMediaAndCourtCase(MediaEntity media, CourtCaseEntity courtCase);

    @Query("""
        SELECT COUNT(DISTINCT cc) = (COUNT(cc.isDataAnonymised) filter (where cc.isDataAnonymised = true))
                    FROM MediaLinkedCaseEntity mlc
                    LEFT JOIN CourtCaseEntity cc ON mlc.courtCase = cc
                    WHERE mlc.media = :mediaEntity
                    group by mlc.media
        """
    )
    boolean areAllAssociatedCasesAnonymised(MediaEntity mediaEntity);
}