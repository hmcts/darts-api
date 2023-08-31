package uk.gov.hmcts.darts.event.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.events.model.CourtLogsPostRequestBody;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DartsEventMapperImplTest {

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";
    private static final String SOME_OTHER_CASE_ID = "2";
    private static final String SOME_TEXT = "some-text";

    private DartsEventMapperImpl dartsEventMapper;

    @BeforeEach
    void setUp() {
        dartsEventMapper = new DartsEventMapperImpl();
    }

    @Test
    void toDartsEventFromCourtLogsPostRequestBodyShouldReturnExpectedMapping() {
        var request = new CourtLogsPostRequestBody(
            SOME_DATE_TIME,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            Arrays.asList(SOME_CASE_ID, SOME_OTHER_CASE_ID),
            SOME_TEXT
        );

        var dartsEvent = dartsEventMapper.toDartsEvent(request);

        assertDoesNotThrow(() -> UUID.fromString(dartsEvent.getMessageId()));
        assertEquals("LOG", dartsEvent.getType());
        assertNull(dartsEvent.getSubType());
        assertNull(dartsEvent.getEventId());
        assertEquals(SOME_COURTHOUSE, dartsEvent.getCourthouse());
        assertEquals(SOME_COURTROOM, dartsEvent.getCourtroom());

        List<String> caseNumbers = dartsEvent.getCaseNumbers();
        assertEquals(2, caseNumbers.size());
        assertThat(caseNumbers, hasItem(SOME_CASE_ID));
        assertThat(caseNumbers, hasItem(SOME_OTHER_CASE_ID));

        assertEquals(SOME_TEXT, dartsEvent.getEventText());
        assertEquals(SOME_DATE_TIME, dartsEvent.getDateTime());
        assertNull(dartsEvent.getRetentionPolicy());
    }

}
