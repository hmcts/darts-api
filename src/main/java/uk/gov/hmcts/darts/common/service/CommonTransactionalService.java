package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.Courthouse;
import uk.gov.hmcts.darts.common.entity.Courtroom;

public interface CommonTransactionalService {
    Courtroom createCourtroom(Courthouse courthouse, String courtroomName);
}
