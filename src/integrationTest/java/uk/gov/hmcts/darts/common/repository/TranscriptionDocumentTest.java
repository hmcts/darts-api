package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionDocumentStub;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentResult;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

class TranscriptionDocumentTest extends PostgresIntegrationBase {

    @Autowired
    private TranscriptionDocumentStub transcriptionStub;

    @Autowired
    private TranscriptionDocumentRepository transcriptionDocumentRepository;

    private List<TranscriptionDocumentEntity> generatedDocumentEntities;

    private static final int GENERATION_COUNT = 2;

    private static final int GENERATION_HEARING_PER_TRANSCRIPTION = 1;

    private static final int GENERATION_CASES_PER_TRANSCRIPTION = 2;

    enum TestType {
        MODENISED, LEGACY
    }

    public List<TranscriptionDocumentEntity> dataSetup(TestType testType) {
        return dataSetup(testType, GENERATION_COUNT, GENERATION_HEARING_PER_TRANSCRIPTION, GENERATION_CASES_PER_TRANSCRIPTION, false, false, true);
    }

    public List<TranscriptionDocumentEntity> dataSetup(TestType testType, int count,
                                                       int hearingCount,
                                                       int caseCount,
                                                       boolean isManualTranscription,
                                                       boolean noCourtHouse,
                                                       boolean associatedWorkflow) {
        if (TestType.MODENISED.equals(testType)) {
            generatedDocumentEntities = transcriptionStub
                .generateTranscriptionEntities(count, hearingCount, caseCount, isManualTranscription, noCourtHouse, associatedWorkflow);
        } else {
            generatedDocumentEntities = transcriptionStub
                .generateTranscriptionEntitiesLegacy(count, caseCount, isManualTranscription, noCourtHouse, associatedWorkflow);
        }
        return generatedDocumentEntities;
    }


    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void findTranscriptionMedia_shouldExcludeHidden_whenShowHiddenIsFalse(TestType testType) {
        dataSetup(testType);
        generatedDocumentEntities.getFirst().setHidden(true);

        dartsDatabase.save(generatedDocumentEntities.getFirst());

        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(null, null, null, null, null, null, null, null, false);


        if (TestType.MODENISED.equals(testType)) {
            //Mod has two items due to joins that are not valid for legacy data
            Assertions.assertEquals(2, transcriptionDocumentResults.size());
        } else {
            Assertions.assertEquals(2, transcriptionDocumentResults.size());
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void findTranscriptionMedia_shouldNotExcludeHidden_whenShowHiddenIsTrue(TestType testType) {
        dataSetup(testType);
        generatedDocumentEntities.getFirst().setHidden(true);

        dartsDatabase.save(generatedDocumentEntities.getFirst());

        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(null, null, null, null, null, null, null, null, true);


        if (TestType.MODENISED.equals(testType)) {
            //Mod has two items due to joins that are not valid for legacy data
            Assertions.assertEquals(4, transcriptionDocumentResults.size());
        } else {
            Assertions.assertEquals(4, transcriptionDocumentResults.size());
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithCaseNumber(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 0;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository
            .findTranscriptionMedia(generatedDocumentEntities.get(nameMatchIndex)
                                        .getTranscription().getCourtCase().getCaseNumber(), null, null, null, null, null, null, null, true);
        if (TestType.MODENISED.equals(testType)) {
            //Mod has two items due to joins that are not valid for legacy data
            Assertions.assertEquals(2, transcriptionDocumentResults.size());
        } else {
            Assertions.assertEquals(1, transcriptionDocumentResults.size());
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void findTranscriptionMedia_shouldExcludeIsCurrentFlase(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 0;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository
            .findTranscriptionMedia(generatedDocumentEntities.get(nameMatchIndex)
                                        .getTranscription().getCourtCase().getCaseNumber(), null, null, null, null, null, null, null, true);
        if (TestType.MODENISED.equals(testType)) {
            //Mod has two items due to joins that are not valid for legacy data
            Assertions.assertEquals(2, transcriptionDocumentResults.size());
        } else {
            Assertions.assertEquals(1, transcriptionDocumentResults.size());
        }

        TranscriptionEntity transcriptionEntity = transactionalUtil.executeInTransaction(() -> {
            //Now set is current = flase should not return any data
            TranscriptionEntity transcription = dartsDatabase.getTranscriptionRepository()
                .findById(generatedDocumentEntities.get(nameMatchIndex)
                              .getTranscription().getId()).orElseThrow();
            transcription.setIsCurrent(false);
            return dartsDatabase.save(transcription);
        });


        transcriptionDocumentResults = transcriptionDocumentRepository
            .findTranscriptionMedia(transcriptionEntity.getCourtCase().getCaseNumber(), null, null, null, null, null, null, null, true);

        Assertions.assertEquals(0, transcriptionDocumentResults.size());

    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithoutAnyParameters(TestType testType) {
        dataSetup(testType);
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(null,
                                                                     null, null, null, null, null, null, null, true);
        Assertions.assertEquals(4, transcriptionDocumentResults.size());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithoutAnyParametersNoHearingCourtCaseWorkflow(TestType testType) {
        dartsDatabase.clearDatabaseInThisOrder();

        generatedDocumentEntities = dataSetup(testType, GENERATION_COUNT, 0, 0,
                                              false, true, false)
            .stream().sorted(Comparator.comparing(TranscriptionDocumentEntity::getId))
            .toList();

        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(null,
                                                                     null, null, null, null, null, null, null, true)
            .stream().sorted(Comparator.comparing(TranscriptionDocumentResult::transcriptionDocumentId))
            .toList();

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults.getFirst(),
                                                     getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                       null)));
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults.get(1),
                                                     getExpectedResult(testType, generatedDocumentEntities.get(1),
                                                                       null)));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithCourtDisplayNameSubstringPrefixMatchOne(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE
                .getQueryStringPrefix(Integer.toString(nameMatchIndex)), null, null, null, null, null, null, true);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex)
                                                                                   .getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithCourtDisplayNameCaseInsensitiveSubstringPrefixMatchOne(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE
                .getQueryStringPrefix(Integer.toString(nameMatchIndex)).toUpperCase(Locale.getDefault()), null, null, null, null, null, null, true);
        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithCourtDisplayNameSubstringPostFixMatchOne(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryStringPostfix(Integer.toString(nameMatchIndex)), null, null,
            null, null, null, null, true);
        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities
                                                                                   .get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });

    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithCourtDisplayNameCaseInsensitiveSubstringPostFixMatchOne(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE
                .getQueryStringPostfix(Integer.toString(nameMatchIndex)).toUpperCase(Locale.getDefault()), null, null, null, null, null, null, true);
        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities
                                                                                   .get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithCourtDisplayNameSubstringMatchAll(TestType testType) {
        dataSetup(testType);
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(), null, null, null, null, null, null, true);

        Assertions.assertEquals(4, transcriptionDocumentResults.size());
        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               generatedDocumentEntities.getFirst()
                                                                                   .getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.getFirst()
                                                                                       .getTranscription().getCourtCases()).get(1)),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(1),
                                                                               generatedDocumentEntities.get(1)
                                                                                   .getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(1),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(1)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithHearingCourthouseDisplayNameNoCaseCourtHouseSubstringMatchAll(TestType testType) {
        dartsDatabase.clearDatabaseInThisOrder();

        generatedDocumentEntities = dataSetup(testType, GENERATION_COUNT, GENERATION_HEARING_PER_TRANSCRIPTION, GENERATION_CASES_PER_TRANSCRIPTION,
                                              false, true, true);

        List<TranscriptionDocumentResult> transcriptionDocumentResults = transcriptionDocumentRepository.findTranscriptionMedia(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(), null, null, null, null, null, null, true);

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               generatedDocumentEntities
                                                                                   .getFirst().getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.getFirst()
                                                                                       .getTranscription().getCourtCases()).get(1)),
                                                             getExpectedResult(testType, generatedDocumentEntities
                                                                                   .get(1),
                                                                               generatedDocumentEntities
                                                                                   .get(1).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(1),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(1)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithNoHearingWithCourtCaseMatchAll(TestType testType) {
        dartsDatabase.clearDatabaseInThisOrder();

        generatedDocumentEntities = dataSetup(testType, GENERATION_COUNT, 0, GENERATION_CASES_PER_TRANSCRIPTION, false, false, true);

        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(), null, null, null, null, null, null, true);

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               generatedDocumentEntities
                                                                                   .getFirst().getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.getFirst()
                                                                                       .getTranscription().getCourtCases()).get(1)),
                                                             getExpectedResult(testType, generatedDocumentEntities
                                                                                   .get(1),
                                                                               generatedDocumentEntities
                                                                                   .get(1).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(1),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(1)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithHearingDate(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null,
            getHearingDate(testType, generatedDocumentEntities.get(1).getTranscription()), null, null, null, null, null, true);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities
                                                                                   .get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    private LocalDate getHearingDate(TestType testType, TranscriptionEntity transcription) {
        if (TestType.MODENISED.equals(testType)) {
            return transcription.getHearing().getHearingDate();
        } else {
            return transcription.getHearingDate();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithManualTranscription(TestType testType) {
        dartsDatabase.clearDatabaseInThisOrder();

        generatedDocumentEntities = dataSetup(testType, 2, 0, 0, false, false, false);

        List<TranscriptionDocumentEntity> generatedDocumentEntitiesWithManualTranscription = dataSetup(testType, 2, 0, 0, true, false, false);

        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null,
            null, null, null, null, true, null, true);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntitiesWithManualTranscription.getFirst(),
                                                                               null),
                                                             getExpectedResult(testType, generatedDocumentEntitiesWithManualTranscription.get(1),
                                                                               null))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithOwnerExact(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            null, null, null, TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryString(Integer.toString(nameMatchIndex)), true);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithOwnerPrefix(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            null, null, null, TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryStringPrefix(Integer.toString(nameMatchIndex)), true);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithOwnerCaseInsensitivePrefix(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            null, null, null,
            TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryStringPrefix(Integer.toString(nameMatchIndex).toLowerCase(Locale.getDefault())), true);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());
        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithOwnerPostfix(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            null, null, null, TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryStringPostfix(Integer.toString(nameMatchIndex)), true);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithOwnerCaseInsensitivePostfix(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            null, null, null,
            TranscriptionDocumentSubStringQueryEnum.OWNER
                .getQueryStringPostfix(Integer.toString(nameMatchIndex).toLowerCase(Locale.getDefault())), true);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithOwnerSubstringMatchAll(TestType testType) {
        dataSetup(testType);
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            null, null, null, TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryStringPrefix(), true);
        Assertions.assertEquals(4, transcriptionDocumentResults.size());

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               generatedDocumentEntities.getFirst().getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.getFirst().getTranscription().getCourtCases()).get(
                                                                                   1)),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(1),
                                                                               generatedDocumentEntities.get(1).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(1),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases()).get(
                                                                                   1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithRequestedByExact(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(null,
                                                                     null, null,
                                                                     TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryString(
                                                                         Integer.toString(nameMatchIndex)), null, null, null, null, true);

        Assertions.assertEquals(2, transcriptionDocumentResults.size());
        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithRequestedByPrefix(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(null,
                                                                     null, null,
                                                                     TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryStringPrefix(
                                                                         Integer.toString(nameMatchIndex)), null, null, null, null, true);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithRequestedByCaseInsensitivePrefix(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(null,
                                                                     null, null,
                                                                     TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryStringPrefix(
                                                                             Integer.toString(nameMatchIndex))
                                                                         .toUpperCase(Locale.getDefault()), null, null, null, null, true);

        Assertions.assertEquals(2, transcriptionDocumentResults.size());
        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithRequestedByPostfix(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null,
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(Integer.toString(1)), null, null, null, null, true);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithRequestedByCaseInsensitivePostfix(TestType testType) {
        dataSetup(testType);
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null,
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY
                .getQueryStringPostfix(Integer.toString(1).toLowerCase(Locale.getDefault())), null, null, null, null, true);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               generatedDocumentEntities
                                                                                   .get(nameMatchIndex).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(nameMatchIndex),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(nameMatchIndex)
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithRequestedBySubstringMatchAll(TestType testType) {
        dataSetup(testType);
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null,
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(), null, null, null, null, true);

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               generatedDocumentEntities.getFirst().getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.getFirst().getTranscription().getCourtCases()).get(
                                                                                   1)),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(1),
                                                                               generatedDocumentEntities.get(1).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(1),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases()).get(
                                                                                   1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithRequestedAtFromAndRequestedAtToSameDay(TestType testType) {
        dataSetup(testType);
        int fromAtPosition = 0;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            generatedDocumentEntities.get(fromAtPosition).getTranscription().getCreatedDateTime(),
            generatedDocumentEntities.get(fromAtPosition).getTranscription().getCreatedDateTime(), null, null, true);

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               generatedDocumentEntities.getFirst().getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.getFirst()
                                                                                       .getTranscription().getCourtCases()).get(1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithRequestedAtFrom(TestType testType) {
        dataSetup(testType);
        int fromAtPosition = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null,
            null, generatedDocumentEntities.get(fromAtPosition).getTranscription().getCreatedDateTime(), null, null, null, true);

        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.get(1),
                                                                               generatedDocumentEntities.get(1).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(1),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases()).get(
                                                                                   1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithRequestedAtTo(TestType testType) {
        dataSetup(testType);
        int toPosition = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMedia(null,
                                                                     null, null,
                                                                     null, null,
                                                                     generatedDocumentEntities.get(toPosition)
                                                                         .getTranscription().getCreatedDateTime(), null, null, true);
        transactionalUtil.executeInTransaction(() -> {
            Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                     List.of(getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               generatedDocumentEntities.getFirst().getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.getFirst(),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.getFirst().getTranscription().getCourtCases()).get(
                                                                                   1)),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(1),
                                                                               generatedDocumentEntities.get(1).getTranscription().getCourtCase()),
                                                             getExpectedResult(testType, generatedDocumentEntities.get(1),
                                                                               TestUtils.getOrderedByCreatedByAndIdInt(
                                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases()).get(
                                                                                   1)))));
        });
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TestType.class)
    void testFindTranscriptionDocumentWithAllQueryParameters(TestType testType) {
        dataSetup(testType);
        UserAccountEntity createdBy = dartsDatabase.getUserAccountRepository()
            .findById(generatedDocumentEntities.getFirst().getTranscription().getCreatedById()).orElseThrow();
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository
            .findTranscriptionMedia(generatedDocumentEntities.getFirst().getTranscription().getCourtCase().getCaseNumber(),
                                    generatedDocumentEntities.getFirst().getTranscription().getCourtroom().getCourthouse().getDisplayName(),
                                    getHearingDate(testType, generatedDocumentEntities.getFirst().getTranscription()),
                                    createdBy.getUserFullName(),
                                    generatedDocumentEntities.getFirst().getTranscription().getCreatedDateTime(),
                                    generatedDocumentEntities.getFirst().getTranscription().getCreatedDateTime(), false,
                                    generatedDocumentEntities.getFirst().getTranscription().getTranscriptionWorkflowEntities()
                                        .getFirst().getWorkflowActor().getUserFullName(),
                                    true);
        if (TestType.MODENISED.equals(testType)) {
            //Mod has two items due to joins that are not valid for legacy data
            Assertions.assertEquals(2, transcriptionDocumentResults.size());
        } else {
            Assertions.assertEquals(1, transcriptionDocumentResults.size());
        }
        Assertions.assertEquals(generatedDocumentEntities.getFirst().getId(),
                                transcriptionDocumentResults.getFirst().transcriptionDocumentId());
    }

    private boolean assertResultEquals(TranscriptionDocumentResult asserted, TranscriptionDocumentResult expected) {
        Assertions.assertEquals(expected.transcriptionDocumentId(), asserted.transcriptionDocumentId());
        Assertions.assertEquals(expected.transcriptionId(), asserted.transcriptionId());
        Assertions.assertEquals(expected.caseNumber(), asserted.caseNumber());
        Assertions.assertEquals(expected.caseId(), asserted.caseId());
        Assertions.assertEquals(expected.courthouseDisplayName(), asserted.courthouseDisplayName());
        Assertions.assertEquals(expected.hearingCourthouseDisplayName(), asserted.hearingCourthouseDisplayName());
        Assertions.assertEquals(expected.hearingDate(), asserted.hearingDate());
        Assertions.assertEquals(expected.isManualTranscription(), asserted.isManualTranscription());
        Assertions.assertEquals(expected.isHidden(), asserted.isHidden());
        return true;
    }

    private boolean assertResultEquals(List<TranscriptionDocumentResult> assertedResults, List<TranscriptionDocumentResult> expected) {
        return assertedResults.containsAll(expected);
    }

    private TranscriptionDocumentResult getExpectedResult(TestType testType, TranscriptionDocumentEntity transcriptionDocument,
                                                          CourtCaseEntity courtCase) {
        Integer caseId = courtCase != null ? courtCase.getId() : null;
        CourtCaseEntity caseEntity = null;
        if (caseId != null) {
            caseEntity = dartsDatabase.getCaseRepository().findById(caseId).orElseThrow();
        }
        TranscriptionDocumentEntity transcriptionDocumentEntity = dartsDatabase.getTranscriptionDocumentRepository()
            .findById(transcriptionDocument.getId()).orElseThrow();

        String caseNumber = caseEntity != null ? caseEntity.getCaseNumber() : null;
        String courthouseDisplayName = getCourthouseDisplayName(caseEntity);

        String hearingCourthouseDisplayName = null;
        LocalDate hearingDate = null;
        String hearingCaseNumber = null;
        Integer hearingCaseId = null;
        Integer hearingId = null;

        if (TestType.MODENISED.equals(testType) && transcriptionDocumentEntity.getTranscription().getHearing() != null) {
            hearingCourthouseDisplayName = transcriptionDocumentEntity.getTranscription().getHearing().getCourtroom().getCourthouse().getDisplayName();
            hearingDate = transcriptionDocumentEntity.getTranscription().getHearing().getHearingDate();
            hearingId = transcriptionDocumentEntity.getTranscription().getHearing().getId();
        } else if (TestType.LEGACY.equals(testType)) {
            hearingDate = transcriptionDocumentEntity.getTranscription().getHearingDate();
        }

        if (transcriptionDocumentEntity.getTranscription().getHearing() != null
            && transcriptionDocumentEntity.getTranscription().getHearing().getCourtCase() != null) {
            CourtCaseEntity hearingCaseEntity = transcriptionDocumentEntity.getTranscription().getHearing().getCourtCase();
            hearingCaseNumber = hearingCaseEntity.getCaseNumber();
            hearingCaseId = hearingCaseEntity.getId();
        }

        return new TranscriptionDocumentResult(transcriptionDocumentEntity.getId(),
                                               transcriptionDocumentEntity.getTranscription().getId(),
                                               caseId,
                                               caseNumber,
                                               hearingId,
                                               hearingCaseId,
                                               hearingCaseNumber,
                                               courthouseDisplayName,
                                               hearingCourthouseDisplayName,
                                               hearingDate,
                                               transcriptionDocumentEntity.getTranscription().getIsManualTranscription(),
                                               transcriptionDocumentEntity.isHidden()
        );
    }

    private String getCourthouseDisplayName(CourtCaseEntity caseEntity) {
        return caseEntity != null && caseEntity.getCourthouse().getCourthouseName() != null
            ? caseEntity.getCourthouse().getCourthouseName() : null;
    }
}