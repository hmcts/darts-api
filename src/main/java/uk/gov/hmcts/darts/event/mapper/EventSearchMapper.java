package uk.gov.hmcts.darts.event.mapper;

import uk.gov.hmcts.darts.event.model.AdminSearchEventResponseResult;
import uk.gov.hmcts.darts.event.model.AdminSearchEventResponseResultCourthouse;
import uk.gov.hmcts.darts.event.model.AdminSearchEventResponseResultCourtroom;
import uk.gov.hmcts.darts.event.model.EventSearchResult;

public final class EventSearchMapper {

    private EventSearchMapper() {
    }

    public static AdminSearchEventResponseResult adminSearchEventResponseResultFrom(EventSearchResult evr) {
        return new AdminSearchEventResponseResult()
            .id(evr.id())
            .eventTs(evr.eventTs())
            .name(evr.eventTypeName())
            .text(evr.eventText())
            .courthouse(buildCourthouse(evr))
            .courtroom(buildCourtroom(evr))
            .isDataAnonymised(evr.isDataAnonymised());
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