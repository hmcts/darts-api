package uk.gov.hmcts.darts.cases.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.RetentionConfidenceCategoryMapperTestData;
import uk.gov.hmcts.darts.test.common.data.builder.TestRetentionConfidenceCategoryMapperEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
class CaseExpiryDeleterIntTest extends IntegrationBase {

    private static final Integer BATCH_SIZE = 5;
    private static final String REQUESTER_EMAIL = "test.user@example.com";
    private static final OffsetDateTime CURRENT_DATE_TIME = OffsetDateTime.of(2025, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final String COURTHOUSE_TO_BE_EXPIRED = "a_courthouse";
    private static final String COURTHOUSE_NOT_TO_BE_EXPIRED = "b_courthouse";
    private static final String CASE_NUMBER_TO_BE_EXPIRED = "019278";
    private static final String CASE_NUMBER_NOT_TO_BE_EXPIRED = "019279";
    private static final String COURTROOM_NAME = "1";
    private static final String TEST_EVENT_NAME_EXPIRED = "testEventName-expired";
    private static final String TEST_EVENT_NAME_NON_EXPIRED = "testEventName-non-expired";

    @Autowired
    private CaseExpiryDeleter caseExpiryDeleter;

    @Autowired
    private DartsPersistence dartsPersistence;

    @Autowired
    private CaseRepository caseRepository;

    private CourtCaseEntity caseToBeExpired;
    private CourtCaseEntity caseNotToBeExpired;
    private EventEntity caseToBeExpiredEvent1;
    private EventEntity caseToBeExpiredEvent2;
    private EventEntity caseToBeExpiredEvent3;
    private EventEntity caseNotToBeExpiredEvent1;
    private EventEntity caseNotToBeExpiredEvent2;
    private EventEntity caseNotToBeExpiredEvent3;

    @TestConfiguration
    static class ClockConfig {
        @Bean
        public Clock clock() {
            return Clock.fixed(CURRENT_DATE_TIME.toInstant(), ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void beforeEach() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(REQUESTER_EMAIL))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        dartsDatabase.createTestUserAccount();

        createAndSaveRetentionConfidenceCategoryMappings();

        OffsetDateTime expiredCaseDateTime = CURRENT_DATE_TIME.minusYears(9);
        HearingEntity caseToBeExpiredHearing = dartsDatabase.createHearing(COURTHOUSE_TO_BE_EXPIRED, COURTROOM_NAME, CASE_NUMBER_TO_BE_EXPIRED,
                                                                           expiredCaseDateTime.toLocalDateTime());
        dartsDatabase.getHearingRepository().saveAndFlush(caseToBeExpiredHearing);

        caseToBeExpiredEvent1 = createEvent(caseToBeExpiredHearing, 8, expiredCaseDateTime.minusMinutes(10), TEST_EVENT_NAME_EXPIRED, false);
        caseToBeExpiredEvent2 = createEvent(caseToBeExpiredHearing, 214, expiredCaseDateTime, TEST_EVENT_NAME_EXPIRED, false);
        caseToBeExpiredEvent3 = createEvent(caseToBeExpiredHearing, 23, expiredCaseDateTime.plusMinutes(10), TEST_EVENT_NAME_EXPIRED, false);
        dartsDatabase.saveAll(caseToBeExpiredEvent1, caseToBeExpiredEvent2, caseToBeExpiredEvent3);
        assertEquals(TEST_EVENT_NAME_EXPIRED, caseToBeExpiredEvent1.getEventText());

        caseToBeExpired = caseToBeExpiredHearing.getCourtCase();
        caseToBeExpired.setCreatedDateTime(OffsetDateTime.now().minusYears(10));
        caseToBeExpired.setClosed(true);
        caseToBeExpired.setCaseClosedTimestamp(OffsetDateTime.now().minusYears(7));
        caseToBeExpired.setDataAnonymised(false);
        dartsDatabase.save(caseToBeExpired);

        // Link events to the case so anonymisation can locate them via event_linked_case.
        dartsDatabase.createEventLinkedCase(caseToBeExpiredEvent1, caseToBeExpired);
        dartsDatabase.createEventLinkedCase(caseToBeExpiredEvent2, caseToBeExpired);
        dartsDatabase.createEventLinkedCase(caseToBeExpiredEvent3, caseToBeExpired);

        OffsetDateTime nonExpiredCaseDateTime = CURRENT_DATE_TIME.minusYears(1);
        HearingEntity caseNotToBeExpiredHearing = dartsDatabase.createHearing(COURTHOUSE_NOT_TO_BE_EXPIRED, COURTROOM_NAME, CASE_NUMBER_NOT_TO_BE_EXPIRED,
                                                                              nonExpiredCaseDateTime.toLocalDateTime());
        dartsDatabase.getHearingRepository().saveAndFlush(caseNotToBeExpiredHearing);

        caseNotToBeExpiredEvent1 = createEvent(caseNotToBeExpiredHearing, 8, nonExpiredCaseDateTime.minusMinutes(10), TEST_EVENT_NAME_NON_EXPIRED, null);
        caseNotToBeExpiredEvent2 = createEvent(caseNotToBeExpiredHearing, 214, nonExpiredCaseDateTime, TEST_EVENT_NAME_NON_EXPIRED, null);
        caseNotToBeExpiredEvent3 = createEvent(caseNotToBeExpiredHearing, 23, nonExpiredCaseDateTime.plusMinutes(10), TEST_EVENT_NAME_NON_EXPIRED, null);
        dartsDatabase.saveAll(caseNotToBeExpiredEvent1, caseNotToBeExpiredEvent2, caseNotToBeExpiredEvent3);

        caseNotToBeExpired = caseNotToBeExpiredHearing.getCourtCase();
        caseNotToBeExpired.setCreatedDateTime(nonExpiredCaseDateTime);
        caseNotToBeExpired.setClosed(true);
        caseNotToBeExpired.setCaseClosedTimestamp(OffsetDateTime.now());
        caseNotToBeExpired.setDataAnonymised(false);
        dartsDatabase.save(caseNotToBeExpired);

        // Link events to the case so we can assert non-expired events stay untouched.
        dartsDatabase.createEventLinkedCase(caseNotToBeExpiredEvent1, caseNotToBeExpired);
        dartsDatabase.createEventLinkedCase(caseNotToBeExpiredEvent2, caseNotToBeExpired);
        dartsDatabase.createEventLinkedCase(caseNotToBeExpiredEvent3, caseNotToBeExpired);

        MediaEntity caseToBeExpiredMedia = PersistableFactory.getMediaTestData().someMinimalBuilderHolder()
            .getBuilder()
            .courtroom(caseToBeExpiredHearing.getCourtroom())
            .build()
            .getEntity();
        MediaEntity caseNotToBeExpiredMedia = PersistableFactory.getMediaTestData().someMinimalBuilderHolder()
            .getBuilder()
            .courtroom(caseNotToBeExpiredHearing.getCourtroom())
            .build()
            .getEntity();

        caseToBeExpiredHearing.addMedia(caseToBeExpiredMedia);
        caseNotToBeExpiredHearing.addMedia(caseNotToBeExpiredMedia);
        dartsDatabase.save(caseToBeExpiredMedia);
        dartsDatabase.save(caseNotToBeExpiredMedia);
        dartsDatabase.save(caseToBeExpiredHearing);
        dartsDatabase.save(caseNotToBeExpiredHearing);

        // Ensure media_linked_case exists; otherwise areAllAssociatedCasesAnonymised(media) returns null in H2
        // (no rows / no group), which triggers an AOP invocation exception and marks the transaction rollback-only.
        dartsDatabase.createMediaLinkedCase(caseToBeExpiredMedia, caseToBeExpired);
        dartsDatabase.createMediaLinkedCase(caseNotToBeExpiredMedia, caseNotToBeExpired);

        dartsDatabase.getCaseRetentionStub().createCaseRetentionObject(caseToBeExpired, CaseRetentionStatus.COMPLETE,
                                                                       CURRENT_DATE_TIME.minusYears(1), false);
        dartsDatabase.getCaseRetentionStub().createCaseRetentionObject(caseNotToBeExpired, CaseRetentionStatus.PENDING,
                                                                       OffsetDateTime.now(), false);

    }

    private EventEntity createEvent(HearingEntity hearing, int eventTypeId, OffsetDateTime createdDateTime, String eventText,
                                    Boolean dataAnonymised) {
        EventEntity event = dartsDatabase.getEventStub().createEvent(hearing, eventTypeId);
        event.setCreatedDateTime(createdDateTime);
        event.setEventText(eventText);
        if (dataAnonymised != null) {
            event.setDataAnonymised(dataAnonymised);
        }
        return event;
    }

    @Test
    void delete_shouldAnonymiseExpiredCasesOnly() {

        // When
        transactionalUtil.executeInTransaction(() -> {
            caseExpiryDeleter.delete(BATCH_SIZE);

            CourtCaseEntity expiredCaseInTransaction = caseRepository.findById(caseToBeExpired.getId()).orElseThrow();
            CourtCaseEntity nonExpiredCaseInTransaction = caseRepository.findById(caseNotToBeExpired.getId()).orElseThrow();

            assertThat(expiredCaseInTransaction.isDataAnonymised()).isTrue();
            assertThat(nonExpiredCaseInTransaction.isDataAnonymised()).isFalse();

            EventEntity expiredEvent1 = dartsDatabase.getEventRepository().findById(caseToBeExpiredEvent1.getId()).orElseThrow();
            assertTrue(expiredEvent1.isDataAnonymised());
            assertNotEquals(TEST_EVENT_NAME_EXPIRED, expiredEvent1.getEventText());

            EventEntity expiredEvent2 = dartsDatabase.getEventRepository().findById(caseToBeExpiredEvent2.getId()).orElseThrow();
            assertTrue(expiredEvent2.isDataAnonymised());
            assertNotEquals(TEST_EVENT_NAME_EXPIRED, expiredEvent2.getEventText());

            EventEntity expiredEvent3 = dartsDatabase.getEventRepository().findById(caseToBeExpiredEvent3.getId()).orElseThrow();
            assertTrue(expiredEvent3.isDataAnonymised());
            assertNotEquals(TEST_EVENT_NAME_EXPIRED, expiredEvent3.getEventText());

            EventEntity nonExpiredEvent1 = dartsDatabase.getEventRepository().findById(caseNotToBeExpiredEvent1.getId()).orElseThrow();
            assertFalse(nonExpiredEvent1.isDataAnonymised());
            assertEquals(TEST_EVENT_NAME_NON_EXPIRED, nonExpiredEvent1.getEventText());

            EventEntity nonExpiredEvent2 = dartsDatabase.getEventRepository().findById(caseNotToBeExpiredEvent2.getId()).orElseThrow();
            assertFalse(nonExpiredEvent2.isDataAnonymised());
            assertEquals(TEST_EVENT_NAME_NON_EXPIRED, nonExpiredEvent2.getEventText());

            EventEntity nonExpiredEvent3 = dartsDatabase.getEventRepository().findById(caseNotToBeExpiredEvent3.getId()).orElseThrow();
            assertFalse(nonExpiredEvent3.isDataAnonymised());
            assertEquals(TEST_EVENT_NAME_NON_EXPIRED, nonExpiredEvent3.getEventText());

        });
    }

    private void createRetentionConfidenceCategoryMapperEntity(RetentionConfidenceCategoryEnum retentionConfidenceCategoryEnum,
                                                               RetentionConfidenceReasonEnum retentionConfidenceReasonEnum,
                                                               RetentionConfidenceScoreEnum retentionConfidenceScoreEnum) {

        RetentionConfidenceCategoryMapperTestData testData = PersistableFactory.getRetentionConfidenceCategoryMapperTestData();
        TestRetentionConfidenceCategoryMapperEntity agedCaseMappingEntity = testData.someMinimalBuilder()
            .confidenceCategory(retentionConfidenceCategoryEnum.getId())
            .confidenceReason(retentionConfidenceReasonEnum)
            .confidenceScore(retentionConfidenceScoreEnum)
            .build();
        dartsPersistence.save(agedCaseMappingEntity.getEntity());
    }

    private void createAndSaveRetentionConfidenceCategoryMappings() {
        createRetentionConfidenceCategoryMapperEntity(
            RetentionConfidenceCategoryEnum.AGED_CASE_CASE_CLOSED,
            RetentionConfidenceReasonEnum.AGED_CASE,
            RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED
        );
        createRetentionConfidenceCategoryMapperEntity(
            RetentionConfidenceCategoryEnum.AGED_CASE_MAX_EVENT_CLOSED,
            RetentionConfidenceReasonEnum.MAX_EVENT_CLOSED,
            RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED
        );
        createRetentionConfidenceCategoryMapperEntity(
            RetentionConfidenceCategoryEnum.AGED_CASE_MAX_MEDIA_CLOSED,
            RetentionConfidenceReasonEnum.MAX_MEDIA_CLOSED,
            RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED
        );
        createRetentionConfidenceCategoryMapperEntity(
            RetentionConfidenceCategoryEnum.AGED_CASE_MAX_HEARING_CLOSED,
            RetentionConfidenceReasonEnum.MAX_HEARING_CLOSED,
            RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED
        );
        createRetentionConfidenceCategoryMapperEntity(
            RetentionConfidenceCategoryEnum.AGED_CASE_CASE_CREATION_CLOSED,
            RetentionConfidenceReasonEnum.CASE_CREATION_CLOSED,
            RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED
        );
        createRetentionConfidenceCategoryMapperEntity(
            RetentionConfidenceCategoryEnum.CASE_CLOSED,
            RetentionConfidenceReasonEnum.CASE_CLOSED,
            RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED
        );
    }
}
