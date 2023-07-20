package uk.gov.hmcts.darts.event.mapper;

import lombok.experimental.UtilityClass;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.CourtLog;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class EventEntityToCourtLogMapper {

    public List<CourtLog> mapFromEntityToCourtLogs(List<EventEntity> entities, String courthouse, String caseNumber) {

        List<CourtLog> logs = new ArrayList<>();

        for (EventEntity entity : entities) {
            logs.add(mapSingleEntityToCourtLog(entity));
        }

        return logs;
    }

    private CourtLog mapSingleEntityToCourtLog(EventEntity entity) {

        CourtLog log = new CourtLog();

        log.setCourthouse(entity.getHearingEntities().get(0).getCourtroom().getCourthouse().getCourthouseName());
        log.setCaseNumber(entity.getHearingEntities().get(0).getCourtCase().getCaseNumber());
        log.setEventText(entity.getEventText());
        log.setTimestamp(entity.getTimestamp());

        return log;

    }

}
