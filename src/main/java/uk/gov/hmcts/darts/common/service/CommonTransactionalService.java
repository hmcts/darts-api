package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;

public interface CommonTransactionalService {
    CourtroomEntity createCourtroom(CourthouseEntity courthouse, String courtroomName);
}
