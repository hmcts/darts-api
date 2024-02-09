package uk.gov.hmcts.darts.event.model;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import uk.gov.hmcts.darts.event.enums.DarNotifyType;

@Getter
public class DarNotifyApplicationEvent extends ApplicationEvent {
    private final DartsEvent dartsEvent;
    private final DarNotifyType darNotifyType;
    private final Integer courtroomId;

    public DarNotifyApplicationEvent(Object source, DartsEvent dartsEvent, DarNotifyType darNotifyType, Integer courtroomId) {
        super(source);
        this.dartsEvent = dartsEvent;
        this.darNotifyType = darNotifyType;
        this.courtroomId = courtroomId;
    }
}
