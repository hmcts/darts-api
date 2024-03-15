package uk.gov.hmcts.darts.cases.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class CloseOldCasesProcessorTest extends IntegrationBase {



    @Autowired
    CloseOldCasesProcessor closeOldCasesProcessor;

    @Test
    void givenClosedEventsUseDateAsClosedDate() {
        HearingEntity hearing =  dartsDatabase.createHearing("a_courthouse", "1", "1078", LocalDate.now().minusYears(7));

        OffsetDateTime closeDate = OffsetDateTime.now().minusYears(7);
        EventEntity eventEntity1 = dartsDatabase.getEventStub().createEvent(hearing, 8);
        eventEntity1.setCreatedDateTime(OffsetDateTime.now().minusYears(7).plusDays(10));
        EventEntity eventEntity2 = dartsDatabase.getEventStub().createEvent(hearing, 214);
        eventEntity2.setCreatedDateTime(closeDate);
        EventEntity eventEntity3 = dartsDatabase.getEventStub().createEvent(hearing, 23);
        eventEntity3.setCreatedDateTime(OffsetDateTime.now().minusYears(7).plusDays(5));
        dartsDatabase.saveAll(eventEntity1, eventEntity2, eventEntity3);

        CourtCaseEntity courtCaseEntity = hearing.getCourtCase();
        courtCaseEntity.setCreatedDateTime(OffsetDateTime.now().minusYears(10));
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());

        closeOldCasesProcessor.closeCases();

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertTrue(updatedCourtCaseEntity.getClosed());
        assertEquals(closeDate.truncatedTo(ChronoUnit.MINUTES).withHour(0).withMinute(0), updatedCourtCaseEntity.getCaseClosedTimestamp());
    }

    @Test
    void givenEventsUseDateAsClosedDate() {
        HearingEntity hearing =  dartsDatabase.createHearing("a_courthouse", "1", "1078", LocalDate.now().minusYears(7));

        OffsetDateTime closeDate = OffsetDateTime.now().minusYears(7);
        EventEntity eventEntity1 = dartsDatabase.getEventStub().createEvent(hearing, 8);
        eventEntity1.setCreatedDateTime(OffsetDateTime.now().minusYears(7).minusDays(10));
        EventEntity eventEntity2 = dartsDatabase.getEventStub().createEvent(hearing, 3);
        eventEntity2.setCreatedDateTime(closeDate);
        EventEntity eventEntity3 = dartsDatabase.getEventStub().createEvent(hearing, 23);
        eventEntity3.setCreatedDateTime(OffsetDateTime.now().minusYears(7).minusDays(5));
        dartsDatabase.saveAll(eventEntity1, eventEntity2, eventEntity3);

        CourtCaseEntity courtCaseEntity = hearing.getCourtCase();
        courtCaseEntity.setCreatedDateTime(OffsetDateTime.now().minusYears(10));
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());

        closeOldCasesProcessor.closeCases();

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertTrue(updatedCourtCaseEntity.getClosed());
        assertEquals(closeDate.truncatedTo(ChronoUnit.MINUTES).withHour(0).withMinute(0), updatedCourtCaseEntity.getCaseClosedTimestamp());
    }

    @Test
    void givenAudioUseDateAsClosedDate() {
        OffsetDateTime closeDate = OffsetDateTime.now().minusYears(6).plusDays(2);
        MediaEntity mediaEntity1 = dartsDatabase.createMediaEntity("acourthosue", "1",
                                                                   OffsetDateTime.now().minusYears(7), OffsetDateTime.now().minusYears(7).plusMinutes(20), 1);
        mediaEntity1.setCreatedDateTime(OffsetDateTime.now().minusYears(6));
        MediaEntity mediaEntity2 = dartsDatabase.createMediaEntity("acourthosue", "1",
                                                                   OffsetDateTime.now().minusYears(7), OffsetDateTime.now().minusYears(7).plusMinutes(20), 1);
        mediaEntity2.setCreatedDateTime(closeDate);
        MediaEntity mediaEntity3 = dartsDatabase.createMediaEntity("acourthosue", "1",
                                                                   OffsetDateTime.now().minusYears(7), OffsetDateTime.now().minusYears(7).plusMinutes(20), 1);
        mediaEntity3.setCreatedDateTime(OffsetDateTime.now().minusYears(6).minusDays(2));
        dartsDatabase.saveAll(mediaEntity1, mediaEntity2, mediaEntity3);

        HearingEntity hearing =  dartsDatabase.createHearing("a_courthouse", "1", "1078", LocalDate.now().minusYears(7));
        hearing.addMedia(mediaEntity1);
        hearing.addMedia(mediaEntity2);
        hearing.addMedia(mediaEntity3);
        dartsDatabase.getHearingRepository().save(hearing);

        CourtCaseEntity courtCaseEntity = hearing.getCourtCase();
        courtCaseEntity.setCreatedDateTime(OffsetDateTime.now().minusYears(10));
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());

        closeOldCasesProcessor.closeCases();

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertTrue(updatedCourtCaseEntity.getClosed());
        assertEquals(closeDate, updatedCourtCaseEntity.getCaseClosedTimestamp());
    }

    @Test
    void givenOnlyHearingUseDateAsClosedDate() {
        OffsetDateTime closeDate = OffsetDateTime.now().minusYears(7);
        dartsDatabase.createHearing("a_courthouse", "1", "1078", closeDate.toLocalDate().minusDays(10));
        HearingEntity hearing =  dartsDatabase.createHearing("a_courthouse", "1", "1078", closeDate.toLocalDate());


        CourtCaseEntity courtCaseEntity = hearing.getCourtCase();
        courtCaseEntity.setCreatedDateTime(OffsetDateTime.now().minusYears(10));
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());

        closeOldCasesProcessor.closeCases();

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertTrue(updatedCourtCaseEntity.getClosed());
        assertEquals(closeDate.toLocalDate().atStartOfDay(), updatedCourtCaseEntity.getCaseClosedTimestamp().toLocalDateTime());
    }

    @Test
    void givenNoDataUseCreatedDateAsClosedDate() {
        OffsetDateTime closeDate = OffsetDateTime.now().minusYears(7);
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("a_courthouse", "019278");
        courtCaseEntity.setCreatedDateTime(closeDate);
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());
        closeOldCasesProcessor.closeCases();

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertTrue(updatedCourtCaseEntity.getClosed());
        assertEquals(closeDate, updatedCourtCaseEntity.getCaseClosedTimestamp());
    }

    @Test
    void givenRetentionPolicyDoNotClose() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("a_courthouse", "019278");
        courtCaseEntity.setCreatedDateTime(OffsetDateTime.now().minusYears(7));
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());

        dartsDatabase.getCaseRetentionStub().createCaseRetentionObject(courtCaseEntity, CaseRetentionStatus.COMPLETE,
                                                                       OffsetDateTime.now().plusYears(7), false);

        closeOldCasesProcessor.closeCases();

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertFalse(updatedCourtCaseEntity.getClosed());
        assertNull(updatedCourtCaseEntity.getCaseClosedTimestamp());
    }

    @Test
    void givenNotSixYearsOldDoNotClose() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("a_courthouse", "019278");
        courtCaseEntity.setCreatedDateTime(OffsetDateTime.now().minusYears(5).minusDays(360));
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());

        closeOldCasesProcessor.closeCases();

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertFalse(updatedCourtCaseEntity.getClosed());
        assertNull(updatedCourtCaseEntity.getCaseClosedTimestamp());
    }
}
