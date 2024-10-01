package uk.gov.hmcts.darts.dets.service;

import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;

public interface ObjectStateRecordService {

    ObjectStateRecordEntity getObjectStateRecordEntityById(Long objectStateRecordId);
    
}
