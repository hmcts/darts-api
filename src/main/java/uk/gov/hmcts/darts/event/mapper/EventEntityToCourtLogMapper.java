package uk.gov.hmcts.darts.event.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.CourtLog;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class EventEntityToCourtLogMapper {

    public List<CourtLog> mapToCourtLogsList(List<EventEntity> eventEntities) {

        List<CourtLog> logs = new ArrayList<>();

        for (EventEntity entity : eventEntities) {
            logs.add(mapToCourtLog(entity));
        }

        return logs;
    }

    private CourtLog mapToCourtLog(EventEntity eventEntity) {

        CourtLog log = new CourtLog();

        log.setCourthouse(eventEntity.associatedHearing().getCourtroom().getCourthouse().getDisplayName());
        log.setCaseNumber(eventEntity.associatedHearing().getCourtCase().getCaseNumber());
        log.setEventText(eventEntity.getEventText());
        log.setTimestamp(eventEntity.getTimestamp());

        return log;

    }

}