package uk.gov.hmcts.darts.cases.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.createEventWith;

@Slf4j
class CaseServiceGetEventsTest extends IntegrationBase {

    @Autowired
    private CaseService service;
    private int caseId;

    private static final String CASE_NUMBER = "CASE1";
    private static final String COURTHOUSE = "SWANSEA";
    private static final String COURTROOM = "1";

    @BeforeEach
    void setupData() {
        HearingEntity hearingEntity1 = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            CASE_NUMBER,
            COURTHOUSE,
            COURTROOM,
            DateConverterUtil.toLocalDateTime(OffsetDateTime.parse("2024-07-01T12:00Z"))
        );
        CourtCaseEntity courtCaseEntity = hearingEntity1.getCourtCase();
        caseId = courtCaseEntity.getId();

        HearingEntity hearingEntity2 = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            CASE_NUMBER,
            COURTHOUSE,
            COURTROOM,
            DateConverterUtil.toLocalDateTime(OffsetDateTime.parse("2024-07-02T12:00Z"))
        );

        EventEntity event1 = createEventWith("eventName", "event1", hearingEntity1, OffsetDateTime.now().minusHours(1));
        EventEntity event2 = createEventWith("eventName", "event2", hearingEntity1, OffsetDateTime.now().minusHours(2));
        EventEntity event3 = createEventWith("eventName", "event3", hearingEntity1, OffsetDateTime.now());

        EventEntity event4 = createEventWith("eventName", "event4", hearingEntity2, OffsetDateTime.now());
        EventEntity event5 = createEventWith("eventName", "event5", hearingEntity2, OffsetDateTime.now().minusHours(1));

        dartsDatabase.saveAll(event1, event2, event3, event4, event5);
    }

    @Test
    void testGetEventsByCaseIdInDescendingDateOrder() {
        var caseEvents = service.getEventsByCaseId(caseId);
        assertEquals(5, caseEvents.size());

        assertThat(caseEvents.get(0).getHearingDate().isAfter(caseEvents.get(1).getHearingDate()));
        assertThat(caseEvents.get(1).getHearingDate().isAfter(caseEvents.get(2).getHearingDate()));
        assertThat(caseEvents.get(2).getHearingDate().isAfter(caseEvents.get(3).getHearingDate()));
        assertThat(caseEvents.get(3).getHearingDate().isAfter(caseEvents.get(4).getHearingDate()));

        assertEquals(4, caseEvents.get(0).getId());
        assertEquals(5, caseEvents.get(1).getId());
        assertEquals(3, caseEvents.get(2).getId());
        assertEquals(1, caseEvents.get(3).getId());
        assertEquals(2, caseEvents.get(4).getId());
    }
}