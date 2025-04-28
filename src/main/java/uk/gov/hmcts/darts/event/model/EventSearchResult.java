package uk.gov.hmcts.darts.event.model;

import java.time.OffsetDateTime;
import java.util.Objects;

public record EventSearchResult(
    Long id,
    OffsetDateTime eventTs,
    String eventTypeName,
    String eventText,
    Boolean isDataAnonymised,
    Integer courtHouseId,
    String courtHouseDisplayName,
    Integer courtroomId,
    String courtroomName,
    Boolean isDataAnonymisedForCase,
    OffsetDateTime dataAnonymisedTs
) {

    @Override
    @SuppressWarnings({"PMD.SimplifyBooleanReturns"})
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof EventSearchResult transcriptionSearchResult)) {
            return false;
        }
        return transcriptionSearchResult.id.equals(this.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}