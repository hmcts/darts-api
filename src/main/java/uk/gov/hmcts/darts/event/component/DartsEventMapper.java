package uk.gov.hmcts.darts.event.component;

import uk.gov.hmcts.darts.event.model.CourtLogsPostRequestBody;
import uk.gov.hmcts.darts.event.model.DartsEvent;

@FunctionalInterface
public interface DartsEventMapper {

    DartsEvent toDartsEvent(CourtLogsPostRequestBody request);

}
