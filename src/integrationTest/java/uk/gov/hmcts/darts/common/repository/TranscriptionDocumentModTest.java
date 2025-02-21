package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionDocumentStub;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentResult;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

class TranscriptionDocumentModTest extends PostgresIntegrationBase {

    @Autowired
    private TranscriptionDocumentStub transcriptionStub;

    @Autowired
    private TranscriptionDocumentRepository transcriptionDocumentRepository;

    private List<TranscriptionDocumentEntity> generatedDocumentEntities;

    private static final int GENERATION_COUNT = 2;

    private static final int GENERATION_HEARING_PER_TRANSCRIPTION = 1;

    private static final int GENERATION_CASES_PER_TRANSCRIPTION = 2;

    @BeforeEach
    public void before() {
        generatedDocumentEntities = transcriptionStub
            .generateTranscriptionEntities(GENERATION_COUNT, GENERATION_HEARING_PER_TRANSCRIPTION, GENERATION_CASES_PER_TRANSCRIPTION, false, false, true);
    }

    @Test
    void testFindTranscriptionDocumentWithCaseNumber() {
        int nameMatchIndex = 0;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository
            .findTranscriptionMediaModenised(generatedDocumentEntities.get(nameMatchIndex)
                                                 .getTranscription().getCourtCases().get(0).getCaseNumber(), null, null, null, null, null, null, null);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());
    }

    @Test
    void testFindTranscriptionDocumentWithoutAnyParameters() {
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(null,
                                                                              null, null, null, null, null, null, null);
        Assertions.assertEquals(4, transcriptionDocumentResults.size());
    }

    @Test
    void testFindTranscriptionDocumentWithoutAnyParametersNoHearingCourtCaseWorkflow() {
        dartsDatabase.clearDatabaseInThisOrder();

        generatedDocumentEntities = transcriptionStub
            .generateTranscriptionEntities(GENERATION_COUNT, 0, 0,
                                           false, true, false)
            .stream().sorted(Comparator.comparing(TranscriptionDocumentEntity::getId))
            .toList();

        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(null,
                                                                              null, null, null, null, null, null, null)
            .stream().sorted(Comparator.comparing(TranscriptionDocumentResult::transcriptionDocumentId))
            .toList();

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   null)));
        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   null)));
    }

    @Test
    void testFindTranscriptionDocumentWithCourtDisplayNameSubstringPrefixMatchOne() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE
                .getQueryStringPrefix(Integer.toString(nameMatchIndex)), null, null, null, null, null, null);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex)
                                                                               .getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithCourtDisplayNameCaseInsensitiveSubstringPrefixMatchOne() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE
                .getQueryStringPrefix(Integer.toString(nameMatchIndex)).toUpperCase(Locale.getDefault()), null, null, null, null, null, null);
        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithCourtDisplayNameSubstringPostFixMatchOne() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryStringPostfix(Integer.toString(nameMatchIndex)), null, null, null, null, null, null);
        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities
                                                                               .get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));

    }

    @Test
    void testFindTranscriptionDocumentWithCourtDisplayNameCaseInsensitiveSubstringPostFixMatchOne() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE
                .getQueryStringPostfix(Integer.toString(nameMatchIndex)).toUpperCase(Locale.getDefault()), null, null, null, null, null, null);
        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities
                                                                               .get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex)
                                                                               .getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithCourtDisplayNameSubstringMatchAll() {
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(), null, null, null, null, null, null);

        Assertions.assertEquals(4, transcriptionDocumentResults.size());
        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities.get(0)
                                                                               .getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities.get(0)
                                                                               .getTranscription().getCourtCases().get(1)),
                                                         getExpectedResult(generatedDocumentEntities.get(1),
                                                                           generatedDocumentEntities.get(1)
                                                                               .getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(1),
                                                                           generatedDocumentEntities.get(1)
                                                                               .getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithHearingCourthouseDisplayNameNoCaseCourtHouseSubstringMatchAll() {
        dartsDatabase.clearDatabaseInThisOrder();

        generatedDocumentEntities = transcriptionStub
            .generateTranscriptionEntities(GENERATION_COUNT, GENERATION_HEARING_PER_TRANSCRIPTION, GENERATION_CASES_PER_TRANSCRIPTION, false, true, true);

        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(), null, null, null, null, null, null);

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities
                                                                               .get(0).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities
                                                                               .get(0).getTranscription().getCourtCases().get(1)),
                                                         getExpectedResult(generatedDocumentEntities
                                                                               .get(1),
                                                                           generatedDocumentEntities
                                                                               .get(1).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(1),
                                                                           generatedDocumentEntities
                                                                               .get(1).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithNoHearingWithCourtCaseMatchAll() {
        dartsDatabase.clearDatabaseInThisOrder();

        generatedDocumentEntities = transcriptionStub
            .generateTranscriptionEntities(GENERATION_COUNT, 0, GENERATION_CASES_PER_TRANSCRIPTION, false, false, true);

        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(), null, null, null, null, null, null);

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities
                                                                               .get(0).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities
                                                                               .get(0).getTranscription().getCourtCases().get(1)),
                                                         getExpectedResult(generatedDocumentEntities
                                                                               .get(1),
                                                                           generatedDocumentEntities
                                                                               .get(1).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(1),
                                                                           generatedDocumentEntities
                                                                               .get(1).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithHearingDate() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, null,
            generatedDocumentEntities.get(1).getTranscription().getHearings().get(0).getHearingDate(), null, null, null, null, null);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities
                                                                               .get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithManualTranscription() {
        dartsDatabase.clearDatabaseInThisOrder();

        generatedDocumentEntities = transcriptionStub
            .generateTranscriptionEntities(2, 0, 0, false, false, false);

        List<TranscriptionDocumentEntity> generatedDocumentEntitiesWithManualTranscription = transcriptionStub
            .generateTranscriptionEntities(2, 0, 0, true, false, false);

        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, null,
            null, null, null, null, true, null);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());


        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntitiesWithManualTranscription.get(0),
                                                                           null),
                                                         getExpectedResult(generatedDocumentEntitiesWithManualTranscription.get(1),
                                                                           null))));
    }

    @Test
    void testFindTranscriptionDocumentWithOwnerExact() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, null, null, null,
            null, null, null, TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryString(Integer.toString(nameMatchIndex)));
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithOwnerPrefix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, null, null, null,
            null, null, null, TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryStringPrefix(Integer.toString(nameMatchIndex)));
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithOwnerCaseInsensitivePrefix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, null, null, null,
            null, null, null,
            TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryStringPrefix(Integer.toString(nameMatchIndex).toLowerCase(Locale.getDefault())));
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithOwnerPostfix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, null, null, null,
            null, null, null, TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryStringPostfix(Integer.toString(nameMatchIndex)));
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithOwnerCaseInsensitivePostfix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, null, null, null,
            null, null, null,
            TranscriptionDocumentSubStringQueryEnum.OWNER
                .getQueryStringPostfix(Integer.toString(nameMatchIndex).toLowerCase(Locale.getDefault())));
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithOwnerSubstringMatchAll() {
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, null, null, null,
            null, null, null, TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryStringPrefix());
        Assertions.assertEquals(4, transcriptionDocumentResults.size());
        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(1)),
                                                         getExpectedResult(generatedDocumentEntities.get(1),
                                                                           generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(1),
                                                                           generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedByExact() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(null,
                                                                              null, null,
                                                                              TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryString(
                                                                                  Integer.toString(nameMatchIndex)), null, null, null, null);

        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedByPrefix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(null,
                                                                              null, null,
                                                                              TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryStringPrefix(
                                                                                  Integer.toString(nameMatchIndex)), null, null, null, null);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedByCaseInsensitivePrefix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null,
            null, null,
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryStringPrefix(Integer.toString(nameMatchIndex))
                .toUpperCase(Locale.getDefault()), null, null, null, null);

        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedByPostfix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, null, null,
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(Integer.toString(1)), null, null, null, null);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedByCaseInsensitivePostfix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, null, null,
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY
                .getQueryStringPostfix(Integer.toString(1).toLowerCase(Locale.getDefault())), null, null, null, null);
        Assertions.assertEquals(2, transcriptionDocumentResults.size());

        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                           generatedDocumentEntities
                                                                               .get(nameMatchIndex).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedBySubstringMatchAll() {
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, null, null,
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(), null, null, null, null);
        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(1)),
                                                         getExpectedResult(generatedDocumentEntities.get(1),
                                                                           generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(1),
                                                                           generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedAtFromAndRequestedAtToSameDay() {
        int fromAtPosition = 0;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, null, null, null,
            generatedDocumentEntities.get(fromAtPosition).getTranscription().getCreatedDateTime(),
            generatedDocumentEntities.get(fromAtPosition).getTranscription().getCreatedDateTime(), null, null);


        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedAtFrom() {
        int fromAtPosition = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(
            null, null, null,
            null, generatedDocumentEntities.get(fromAtPosition).getTranscription().getCreatedDateTime(), null, null, null);
        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(1),
                                                                           generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(1),
                                                                           generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedAtTo() {
        int toPosition = 1;
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository.findTranscriptionMediaModenised(null,
                                                                              null, null,
                                                                              null, null,
                                                                              generatedDocumentEntities.get(toPosition)
                                                                                  .getTranscription().getCreatedDateTime(), null, null);
        Assertions.assertTrue(assertResultEquals(transcriptionDocumentResults,
                                                 List.of(getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(0),
                                                                           generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(1)),
                                                         getExpectedResult(generatedDocumentEntities.get(1),
                                                                           generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(0)),
                                                         getExpectedResult(generatedDocumentEntities.get(1),
                                                                           generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(1)))));
    }

    @Test
    void testFindTranscriptionDocumentWithAllQueryParameters() {
        List<TranscriptionDocumentResult> transcriptionDocumentResults
            = transcriptionDocumentRepository
            .findTranscriptionMediaModenised(generatedDocumentEntities.get(0).getTranscription()
                                                 .getCourtCase().getCaseNumber(),
                                             generatedDocumentEntities.get(0).getTranscription().getCourtroom().getCourthouse().getDisplayName(),
                                             generatedDocumentEntities.get(0).getTranscription().getHearings().get(0).getHearingDate(),
                                             generatedDocumentEntities.get(0).getTranscription().getCreatedBy().getUserFullName(),
                                             generatedDocumentEntities.get(0).getTranscription().getCreatedDateTime(),
                                             generatedDocumentEntities.get(0).getTranscription().getCreatedDateTime(), false,
                                             generatedDocumentEntities.get(0).getTranscription().getTranscriptionWorkflowEntities()
                                                 .get(0).getWorkflowActor().getUserFullName());
        Assertions.assertEquals(2, transcriptionDocumentResults.size());
        Assertions.assertEquals(generatedDocumentEntities.get(0).getId(),
                                transcriptionDocumentResults.get(0).transcriptionDocumentId());
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

    private TranscriptionDocumentResult getExpectedResult(TranscriptionDocumentEntity transcriptionDocumentEntity, CourtCaseEntity caseEntity) {
        Integer caseId = caseEntity != null ? caseEntity.getId() : null;
        String caseNumber = caseEntity != null ? caseEntity.getCaseNumber() : null;
        String courthouseDisplayName = getCourthouseDisplayName(caseEntity);

        String hearingCourthouseDisplayName = null;
        LocalDate hearingDate = null;
        String hearingCaseNumber = null;

        if (transcriptionDocumentEntity.getTranscription().getHearing() != null) {
            hearingCourthouseDisplayName = transcriptionDocumentEntity.getTranscription().getHearing().getCourtroom().getCourthouse().getDisplayName();
            hearingDate = transcriptionDocumentEntity.getTranscription().getHearing().getHearingDate();
        }

        if (transcriptionDocumentEntity.getTranscription().getHearing() != null
            && transcriptionDocumentEntity.getTranscription().getHearing().getCourtCase() != null) {
            hearingCaseNumber = transcriptionDocumentEntity.getTranscription().getHearing().getCourtCase().getCaseNumber();
        }

        return new TranscriptionDocumentResult(transcriptionDocumentEntity.getId(),
                                               transcriptionDocumentEntity.getTranscription().getId(),
                                               caseId,
                                               caseNumber,
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