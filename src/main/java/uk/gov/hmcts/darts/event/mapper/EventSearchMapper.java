package uk.gov.hmcts.darts.event.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.event.model.AdminSearchEventResponseResult;
import uk.gov.hmcts.darts.event.model.AdminSearchEventResponseResultCourthouse;
import uk.gov.hmcts.darts.event.model.AdminSearchEventResponseResultCourtroom;
import uk.gov.hmcts.darts.event.model.EventSearchResult;

@Component
public class EventSearchMapper {

    public AdminSearchEventResponseResult adminSearchEventResponseResultFrom(EventSearchResult evr) {
        return new AdminSearchEventResponseResult()
            .id(evr.id())
            .createdAt(evr.createdAt())
            .name(evr.eventTypeName())
            .text(evr.eventText())
            .chronicleId(evr.chronicleId())
            .antecedentId(evr.antecedentId())
            .courthouse(buildCourthouse(evr))
            .courtroom(buildCourtroom(evr));
    }

    private static AdminSearchEventResponseResultCourtroom buildCourtroom(EventSearchResult evr) {
        return new AdminSearchEventResponseResultCourtroom()
            .id(evr.courtroomId())
            .name(evr.courtroomName());
    }

    private static AdminSearchEventResponseResultCourthouse buildCourthouse(EventSearchResult evr) {
        return new AdminSearchEventResponseResultCourthouse()
            .id(evr.courtHouseId())
            .displayName(evr.courtHouseDisplayName());
    }
}
