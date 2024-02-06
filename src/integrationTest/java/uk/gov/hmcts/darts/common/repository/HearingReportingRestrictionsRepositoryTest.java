package uk.gov.hmcts.darts.common.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingReportingRestrictionsEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class HearingReportingRestrictionsRepositoryTest extends IntegrationBase {

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";

    @BeforeEach
    void setUp() {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
              SOME_CASE_ID,
              SOME_COURTHOUSE,
              SOME_COURTROOM,
              SOME_DATE_TIME.toLocalDate()
        );

        //54 and 188 are is_reporting_restriction = true
        dartsDatabase.createEvent(hearingEntity, 54);
        dartsDatabase.createEvent(hearingEntity, 54);
        dartsDatabase.createEvent(hearingEntity, 32);
        dartsDatabase.createEvent(hearingEntity, 68);
        dartsDatabase.createEvent(hearingEntity, 188);
    }

    @Test
    void checkViewExists() {
        List<HearingReportingRestrictionsEntity> hearingReportingRestrictionsEntities
              = dartsDatabase.getHearingReportingRestrictionsRepository().findAll();

        assertEquals(3, hearingReportingRestrictionsEntities.size());
    }
}
