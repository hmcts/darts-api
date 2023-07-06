package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.event.enums.DarNotifyType;
import uk.gov.hmcts.darts.event.model.DartsEvent;

public interface DarNotifyService {

    void darNotify(DartsEvent dartsEvent, DarNotifyType darNotifyType);

}
