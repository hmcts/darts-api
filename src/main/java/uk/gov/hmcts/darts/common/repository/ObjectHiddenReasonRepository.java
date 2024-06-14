package uk.gov.hmcts.darts.common.repository;

import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.repository.base.ReadOnlyRepository;

@Repository
public interface ObjectHiddenReasonRepository extends ReadOnlyRepository<ObjectHiddenReasonEntity, Integer> {
}
