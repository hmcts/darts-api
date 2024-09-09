package uk.gov.hmcts.darts.cases.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Impacted by V1_367__adding_not_null_constraints_part_4.sql")
@Slf4j
class CloseOldCasesProcessorTest extends IntegrationBase {
    @Autowired
    CloseOldCasesProcessor closeOldCasesProcessor;

    private static final Integer BATCH_SIZE = 5;

    private static final String REQUESTER_EMAIL = "test.user@example.com";

    @BeforeEach
    void beforeEach() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(REQUESTER_EMAIL))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        dartsDatabase.createTestUserAccount();
    }

    @Test
    void givenClosedEventsUseDateAsClosedDate() {
        HearingEntity hearing = dartsDatabase.createHearing("a_courthouse", "1", "1078", LocalDateTime.now().minusYears(7).plusMonths(3));

        OffsetDateTime closeDate = OffsetDateTime.now().minusYears(7);

        EventEntity eventEntity1 = dartsDatabase.getEventStub().createEvent(hearing, 8);//Re-examination
        eventEntity1.setCreatedDateTime(OffsetDateTime.now().minusYears(7).plusDays(10));
        EventEntity eventEntity2 = dartsDatabase.getEventStub().createEvent(hearing, 214);//case closed
        eventEntity2.setCreatedDateTime(closeDate);
        EventEntity eventEntity3 = dartsDatabase.getEventStub().createEvent(hearing, 23);//Application: No case to answer
        eventEntity3.setCreatedDateTime(OffsetDateTime.now().minusYears(7).plusDays(5));
        dartsDatabase.saveAll(eventEntity1, eventEntity2, eventEntity3);

        CourtCaseEntity courtCaseEntity = hearing.getCourtCase();
        courtCaseEntity.setCreatedDateTime(OffsetDateTime.now().minusYears(10));
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());

        closeOldCasesProcessor.closeCases(BATCH_SIZE);

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertTrue(updatedCourtCaseEntity.getClosed());
        assertEquals(closeDate.truncatedTo(ChronoUnit.MINUTES),
                     updatedCourtCaseEntity.getCaseClosedTimestamp().truncatedTo(ChronoUnit.MINUTES));
        assertEquals(RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED, updatedCourtCaseEntity.getRetConfScore());
        assertEquals(RetentionConfidenceReasonEnum.AGED_CASE, updatedCourtCaseEntity.getRetConfReason());
        CaseRetentionEntity caseRetentionEntity = dartsDatabase.getCaseRetentionRepository().findAll().get(0);
        assertEquals(courtCaseEntity.getId(), caseRetentionEntity.getCourtCase().getId());
        assertEquals(closeDate.plusYears(7).truncatedTo(ChronoUnit.DAYS), caseRetentionEntity.getRetainUntil());
        assertEquals(RetentionConfidenceCategoryEnum.AGED_CASE, caseRetentionEntity.getConfidenceCategory());
    }

    @Test
    void givenEventsUseLatestDateAsClosedDate() {
        HearingEntity hearing = dartsDatabase.createHearing("a_courthouse", "1", "1078", LocalDateTime.now().minusYears(7));

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

        closeOldCasesProcessor.closeCases(BATCH_SIZE);

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertTrue(updatedCourtCaseEntity.getClosed());
        assertEquals(closeDate.truncatedTo(ChronoUnit.MINUTES), updatedCourtCaseEntity.getCaseClosedTimestamp().truncatedTo(ChronoUnit.MINUTES));
        assertEquals(RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED, updatedCourtCaseEntity.getRetConfScore());
        assertEquals(RetentionConfidenceReasonEnum.AGED_CASE, updatedCourtCaseEntity.getRetConfReason());
        CaseRetentionEntity caseRetentionEntity = dartsDatabase.getCaseRetentionRepository().findAll().get(0);
        assertEquals(courtCaseEntity.getId(), caseRetentionEntity.getCourtCase().getId());
        assertEquals(RetentionConfidenceCategoryEnum.AGED_CASE, caseRetentionEntity.getConfidenceCategory());
    }

    @Test
    void givenOneEventUseLatestDateAsClosedDate() {
        HearingEntity hearing = dartsDatabase.createHearing("a_courthouse", "1", "1078", LocalDateTime.now().minusYears(7));

        OffsetDateTime closeDate = OffsetDateTime.now().minusYears(7);
        EventEntity eventEntity2 = dartsDatabase.getEventStub().createEvent(hearing, 3);
        eventEntity2.setCreatedDateTime(closeDate);

        dartsDatabase.save(eventEntity2);

        CourtCaseEntity courtCaseEntity = hearing.getCourtCase();
        courtCaseEntity.setCreatedDateTime(OffsetDateTime.now().minusYears(10));
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());

        closeOldCasesProcessor.closeCases(BATCH_SIZE);

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertTrue(updatedCourtCaseEntity.getClosed());
        assertEquals(closeDate.truncatedTo(ChronoUnit.MINUTES), updatedCourtCaseEntity.getCaseClosedTimestamp().truncatedTo(ChronoUnit.MINUTES));
        assertEquals(RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED, updatedCourtCaseEntity.getRetConfScore());
        assertEquals(RetentionConfidenceReasonEnum.AGED_CASE, updatedCourtCaseEntity.getRetConfReason());
        CaseRetentionEntity caseRetentionEntity = dartsDatabase.getCaseRetentionRepository().findAll().get(0);
        assertEquals(courtCaseEntity.getId(), caseRetentionEntity.getCourtCase().getId());
        assertEquals(RetentionConfidenceCategoryEnum.AGED_CASE, caseRetentionEntity.getConfidenceCategory());
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

        HearingEntity hearing = dartsDatabase.createHearing("a_courthouse", "1", "1078", LocalDateTime.now().minusYears(7));
        hearing.addMedia(mediaEntity1);
        hearing.addMedia(mediaEntity2);
        hearing.addMedia(mediaEntity3);
        dartsDatabase.getHearingRepository().save(hearing);

        CourtCaseEntity courtCaseEntity = hearing.getCourtCase();
        courtCaseEntity.setCreatedDateTime(OffsetDateTime.now().minusYears(10));
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());

        closeOldCasesProcessor.closeCases(BATCH_SIZE);

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertTrue(updatedCourtCaseEntity.getClosed());
        assertEquals(closeDate.truncatedTo(ChronoUnit.MINUTES), updatedCourtCaseEntity.getCaseClosedTimestamp().truncatedTo(ChronoUnit.MINUTES));
        assertEquals(RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED, updatedCourtCaseEntity.getRetConfScore());
        assertEquals(RetentionConfidenceReasonEnum.AGED_CASE, updatedCourtCaseEntity.getRetConfReason());
        CaseRetentionEntity caseRetentionEntity = dartsDatabase.getCaseRetentionRepository().findAll().get(0);
        assertEquals(courtCaseEntity.getId(), caseRetentionEntity.getCourtCase().getId());
        assertEquals(RetentionConfidenceCategoryEnum.AGED_CASE, caseRetentionEntity.getConfidenceCategory());
    }

    @Test
    void givenOnlyHearingUseDateAsClosedDate() {
        LocalDateTime closeDate = DateConverterUtil.toLocalDateTime(OffsetDateTime.now().minusYears(7));
        dartsDatabase.createHearing("a_courthouse", "1", "1078", closeDate.minusDays(10));
        HearingEntity hearing = dartsDatabase.createHearing("a_courthouse", "1", "1078", closeDate);


        CourtCaseEntity courtCaseEntity = hearing.getCourtCase();
        courtCaseEntity.setCreatedDateTime(OffsetDateTime.now().minusYears(10));
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());

        closeOldCasesProcessor.closeCases(BATCH_SIZE);

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertTrue(updatedCourtCaseEntity.getClosed());
        assertEquals(closeDate.toLocalDate().atStartOfDay(), updatedCourtCaseEntity.getCaseClosedTimestamp().toLocalDateTime());
        assertEquals(RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED, updatedCourtCaseEntity.getRetConfScore());
        assertEquals(RetentionConfidenceReasonEnum.AGED_CASE, updatedCourtCaseEntity.getRetConfReason());
        CaseRetentionEntity caseRetentionEntity = dartsDatabase.getCaseRetentionRepository().findAll().get(0);
        assertEquals(courtCaseEntity.getId(), caseRetentionEntity.getCourtCase().getId());
        assertEquals(RetentionConfidenceCategoryEnum.AGED_CASE, caseRetentionEntity.getConfidenceCategory());
    }

    @Test
    void givenNoDataUseCreatedDateAsClosedDate() {
        OffsetDateTime closeDate = OffsetDateTime.now().minusYears(7);
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("a_courthouse", "019278");
        courtCaseEntity.setCreatedDateTime(closeDate);
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());
        closeOldCasesProcessor.closeCases(BATCH_SIZE);

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertTrue(updatedCourtCaseEntity.getClosed());
        assertEquals(closeDate.truncatedTo(ChronoUnit.MINUTES), updatedCourtCaseEntity.getCaseClosedTimestamp().truncatedTo(ChronoUnit.MINUTES));
        assertEquals(RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED, updatedCourtCaseEntity.getRetConfScore());
        assertEquals(RetentionConfidenceReasonEnum.AGED_CASE, updatedCourtCaseEntity.getRetConfReason());
        CaseRetentionEntity caseRetentionEntity = dartsDatabase.getCaseRetentionRepository().findAll().get(0);
        assertEquals(courtCaseEntity.getId(), caseRetentionEntity.getCourtCase().getId());
        assertEquals(RetentionConfidenceCategoryEnum.AGED_CASE, caseRetentionEntity.getConfidenceCategory());
    }

    @Test
    void givenNoDataUseCreatedDateAsClosedDateUseBatchSizeTwo() {
        // given
        OffsetDateTime closeDate1 = OffsetDateTime.now().minusYears(9);
        CourtCaseEntity courtCaseEntity1 = dartsDatabase.createCase("a_courthouse", "019278");
        courtCaseEntity1.setCreatedDateTime(closeDate1);
        dartsDatabase.getCaseRepository().save(courtCaseEntity1);
        assertFalse(courtCaseEntity1.getClosed());

        OffsetDateTime closeDate2 = OffsetDateTime.now().minusYears(8);
        CourtCaseEntity courtCaseEntity2 = dartsDatabase.createCase("b_courthouse", "019279");
        courtCaseEntity2.setCreatedDateTime(closeDate2);
        dartsDatabase.getCaseRepository().save(courtCaseEntity2);
        assertFalse(courtCaseEntity2.getClosed());

        OffsetDateTime closeDate3 = OffsetDateTime.now().minusYears(7);
        CourtCaseEntity courtCaseEntity3 = dartsDatabase.createCase("c_courthouse", "019280");
        courtCaseEntity3.setCreatedDateTime(closeDate3);
        dartsDatabase.getCaseRepository().save(courtCaseEntity3);
        assertFalse(courtCaseEntity3.getClosed());

        // when
        closeOldCasesProcessor.closeCases(2);

        // then
        CourtCaseEntity updatedCourtCaseEntity1 = dartsDatabase.getCaseRepository().findById(courtCaseEntity1.getId()).orElse(null);
        assert updatedCourtCaseEntity1 != null;
        assertTrue(updatedCourtCaseEntity1.getClosed());
        assertEquals(closeDate1.truncatedTo(ChronoUnit.MINUTES), updatedCourtCaseEntity1.getCaseClosedTimestamp().truncatedTo(ChronoUnit.MINUTES));
        assertEquals(RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED, updatedCourtCaseEntity1.getRetConfScore());
        assertEquals(RetentionConfidenceReasonEnum.AGED_CASE, updatedCourtCaseEntity1.getRetConfReason());
        CaseRetentionEntity caseRetentionEntity = dartsDatabase.getCaseRetentionRepository().findAll().get(0);
        assertEquals(courtCaseEntity1.getId(), caseRetentionEntity.getCourtCase().getId());
        assertEquals(RetentionConfidenceCategoryEnum.AGED_CASE, caseRetentionEntity.getConfidenceCategory());

        CourtCaseEntity updatedCourtCaseEntity2 = dartsDatabase.getCaseRepository().findById(courtCaseEntity2.getId()).orElse(null);
        assert updatedCourtCaseEntity2 != null;
        assertTrue(updatedCourtCaseEntity2.getClosed());

        CourtCaseEntity updatedCourtCaseEntity3 = dartsDatabase.getCaseRepository().findById(courtCaseEntity3.getId()).orElse(null);
        assert updatedCourtCaseEntity3 != null;
        assertFalse(updatedCourtCaseEntity3.getClosed());
    }

    @Test
    void givenRetentionPolicyDoNotClose() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("a_courthouse", "019278");
        courtCaseEntity.setCreatedDateTime(OffsetDateTime.now().minusYears(7));
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());

        dartsDatabase.getCaseRetentionStub().createCaseRetentionObject(courtCaseEntity, CaseRetentionStatus.COMPLETE,
                                                                       OffsetDateTime.now().plusYears(7), false);

        closeOldCasesProcessor.closeCases(BATCH_SIZE);

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertFalse(updatedCourtCaseEntity.getClosed());
        assertNull(updatedCourtCaseEntity.getCaseClosedTimestamp());
        assertNull(updatedCourtCaseEntity.getRetConfScore());
        assertNull(updatedCourtCaseEntity.getRetConfReason());
        CaseRetentionEntity caseRetentionEntity = dartsDatabase.getCaseRetentionRepository().findAll().get(0);
        assertEquals(courtCaseEntity.getId(), caseRetentionEntity.getCourtCase().getId());
        assertNull(caseRetentionEntity.getConfidenceCategory());
    }

    @Test
    void givenNotSixYearsOldDoNotClose() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("a_courthouse", "019278");
        courtCaseEntity.setCreatedDateTime(OffsetDateTime.now().minusYears(5).minusDays(360));
        dartsDatabase.getCaseRepository().save(courtCaseEntity);
        assertFalse(courtCaseEntity.getClosed());

        closeOldCasesProcessor.closeCases(BATCH_SIZE);

        CourtCaseEntity updatedCourtCaseEntity = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).orElse(null);
        assert updatedCourtCaseEntity != null;
        assertFalse(updatedCourtCaseEntity.getClosed());
        assertNull(updatedCourtCaseEntity.getCaseClosedTimestamp());
        assertNull(updatedCourtCaseEntity.getRetConfScore());
        assertNull(updatedCourtCaseEntity.getRetConfReason());
    }
}