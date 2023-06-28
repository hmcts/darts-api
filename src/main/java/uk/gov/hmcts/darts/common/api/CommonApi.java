package uk.gov.hmcts.darts.common.api;

import uk.gov.hmcts.darts.common.entity.CourtroomEntity;

public interface CommonApi {

    CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName);
}
