package uk.gov.hmcts.darts.event.component;

import uk.gov.hmcts.darts.events.model.CourtLogsPostRequestBody;
import uk.gov.hmcts.darts.events.model.DartsEvent;

public interface DartsEventMapper {

    DartsEvent toDartsEvent(CourtLogsPostRequestBody request);

}
