package uk.gov.hmcts.darts.event.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.event.model.EventSearchResult;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventSearchMapperTest {

    @Test
    void mapsEventSearchResultToAdminSearchEventResponse() {
        var eventSearchResult = someEventSearchResult();

        var result = EventSearchMapper.adminSearchEventResponseResultFrom(eventSearchResult);

        assertThat(result.getId()).isEqualTo(eventSearchResult.id());
        assertThat(result.getName()).isEqualTo(eventSearchResult.eventTypeName());
        assertThat(result.getText()).isEqualTo(eventSearchResult.eventText());
        assertThat(result.getCourthouse().getId()).isEqualTo(eventSearchResult.courtHouseId());
        assertThat(result.getCourthouse().getDisplayName()).isEqualTo(eventSearchResult.courtHouseDisplayName());
        assertThat(result.getCourtroom().getId()).isEqualTo(eventSearchResult.courtroomId());
        assertThat(result.getCourtroom().getName()).isEqualTo(eventSearchResult.courtroomName());
        assertThat(result.getIsDataAnonymised()).isEqualTo(eventSearchResult.isDataAnonymised());
    }

    private static EventSearchResult someEventSearchResult() {
        return new EventSearchResult(
            1,
            OffsetDateTime.now(),
            "eventTypeName",
            "eventText",
            true,
            2,
            "courtHouseDisplayName",
            3,
            "courtroomName",
            false,
            null
        );
    }
}