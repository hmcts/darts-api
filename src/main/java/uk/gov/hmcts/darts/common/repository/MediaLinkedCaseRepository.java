package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;

import java.util.List;

@Repository
public interface MediaLinkedCaseRepository extends JpaRepository<MediaLinkedCaseEntity, Integer> {

    List<MediaLinkedCaseEntity> findByMedia(MediaEntity media);

    List<MediaLinkedCaseEntity> findByMediaAndSource(MediaEntity mediaEntity, MediaLinkedCaseSourceType source);

    boolean existsByMediaAndCourtCase(MediaEntity media, CourtCaseEntity courtCase);

}