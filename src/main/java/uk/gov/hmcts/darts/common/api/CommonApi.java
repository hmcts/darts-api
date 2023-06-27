package uk.gov.hmcts.darts.common.api;

import uk.gov.hmcts.darts.common.entity.Courtroom;

public interface CommonApi {

    Courtroom retrieveOrCreateCourtroom(String courthouseName, String courtroomName);
}
