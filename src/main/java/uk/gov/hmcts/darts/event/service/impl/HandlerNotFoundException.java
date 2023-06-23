package uk.gov.hmcts.darts.event.service.impl;

import uk.gov.hmcts.darts.event.model.DartsEvent;

public class HandlerNotFoundException extends RuntimeException {

    public static final String LOG_MESSAGE_FORMAT = "No event handler could be found for message: %s type: %s and subtype: %s";

    public HandlerNotFoundException(DartsEvent event) {
        super(String.format(LOG_MESSAGE_FORMAT, event.getMessageId(), event.getType(), event.getSubType()));
    }
}
