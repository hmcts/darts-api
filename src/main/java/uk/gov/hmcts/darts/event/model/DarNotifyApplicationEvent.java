package uk.gov.hmcts.darts.event.model;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import uk.gov.hmcts.darts.event.enums.DarNotifyType;

@Getter
public class DarNotifyApplicationEvent extends ApplicationEvent {

    private final DartsEvent dartsEvent;
    private final DarNotifyType darNotifyType;

    public DarNotifyApplicationEvent(Object source, DartsEvent dartsEvent, DarNotifyType darNotifyType) {
        super(source);
        this.dartsEvent = dartsEvent;
        this.darNotifyType = darNotifyType;
    }
}
