package uk.gov.hmcts.darts.event.component.impl;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.event.component.DartsEventMapper;
import uk.gov.hmcts.darts.events.model.CourtLogsPostRequestBody;
import uk.gov.hmcts.darts.events.model.DartsEvent;

import java.util.UUID;

@Component
public class DartsEventMapperImpl implements DartsEventMapper {

    public static final String COURTLOG_MESSAGE_TYPE = "LOG";

    @Override
    public DartsEvent toDartsEvent(CourtLogsPostRequestBody request) {
        var dartsEvent = new DartsEvent();
        dartsEvent.setMessageId(UUID.randomUUID()
                                    .toString());

        dartsEvent.setType(COURTLOG_MESSAGE_TYPE);
        dartsEvent.setSubType(null);
        dartsEvent.setEventId(null);
        dartsEvent.setCourthouse(request.getCourthouse());
        dartsEvent.setCourtroom(request.getCourtroom());
        dartsEvent.setCaseNumbers(request.getCaseNumbers());
        dartsEvent.setEventText(request.getText());
        dartsEvent.setDateTime(request.getLogEntryDateTime());
        dartsEvent.setRetentionPolicy(null);

        return dartsEvent;
    }

}
