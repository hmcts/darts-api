package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentResult;

import java.util.List;
import java.util.Locale;

public class TranscriptionDocumentTest extends IntegrationBase {

    @Autowired
    private TranscriptionStub transcriptionStub;

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
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository
            .findTranscriptionMedia(generatedDocumentEntities.get(nameMatchIndex)
                                        .getTranscription().getCourtCases().get(0).getCaseNumber(),null,null,null,null,null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
    }

    @Test
    void testFindTranscriptionDocumentWithoutAnyParameters() {
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(null,
                                                                     null,null,null,null,null, null, null);
        Assertions.assertEquals(4, transformedMediaEntityList.size());
    }

    @Test
    void testFindTranscriptionDocumentWithoutAnyParametersNoHearingCourtCaseWorkflow() {
        dartsDatabase.clearDatabaseInThisOrder();;

        generatedDocumentEntities = transcriptionStub
            .generateTranscriptionEntities(GENERATION_COUNT, 0, 0,
                                           false, true, false);

        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(null,
                                                                      null,null,null,null,null, null, null);

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   null)));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   null)));

    }

    @Test
    void testFindTranscriptionDocumentWithCourtDisplayNameSubstringPrefixMatchOne() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE
                .getQueryStringPrefix(Integer.toString(nameMatchIndex)), null, null, null, null, null, null);
        Assertions.assertEquals(2, transformedMediaEntityList.size());

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithCourtDisplayNameCaseInsensitiveSubstringPrefixMatchOne() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE
                .getQueryStringPrefix(Integer.toString(nameMatchIndex)).toUpperCase(Locale.getDefault()), null, null, null, null, null, null);
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));

    }

    @Test
    void testFindTranscriptionDocumentWithCourtDisplayNameSubstringPostFixMatchOne() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryStringPostfix(Integer.toString(nameMatchIndex)), null, null, null, null, null, null);
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));

    }

    @Test
    void testFindTranscriptionDocumentWithCourtDisplayNameCaseInsensitiveSubstringPostFixMatchOne() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE
                .getQueryStringPostfix(Integer.toString(nameMatchIndex)).toUpperCase(Locale.getDefault()), null, null, null, null, null, null);
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithCourtDisplayNameSubstringMatchAll() {
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(), null, null, null, null, null, null);

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(1))));

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(2),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(3),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithHearingCourthouseDisplayNameNoTranscriptionCourtHouseSubstringMatchAll() {
        dartsDatabase.clearDatabaseInThisOrder();

        generatedDocumentEntities = transcriptionStub
            .generateTranscriptionEntities(GENERATION_COUNT, GENERATION_HEARING_PER_TRANSCRIPTION, GENERATION_CASES_PER_TRANSCRIPTION, false, true, true);

        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,
            TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(), null, null, null, null, null, null);

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(1))));

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(2),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(3),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithHearingDate() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null,
            generatedDocumentEntities.get(1).getTranscription().getHearings().get(0).getHearingDate(), null, null, null, null, null);
        Assertions.assertEquals(2, transformedMediaEntityList.size());

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithManualTranscription() {

        dartsDatabase.clearDatabaseInThisOrder();;

        generatedDocumentEntities = transcriptionStub
            .generateTranscriptionEntities(2, 0, 0, false, false, false);

       List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null,
            null, null, null, null, false, null);
        Assertions.assertEquals(2, transformedMediaEntityList.size());
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   null)));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   null)));
    }

    @Test
    void testFindTranscriptionDocumentWithOwnerExact() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            null, null, null,  TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryString(Integer.toString(nameMatchIndex)));
        Assertions.assertEquals(2, transformedMediaEntityList.size());

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithOwnerPrefix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            null, null, null,  TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryStringPrefix(Integer.toString(nameMatchIndex)));
        Assertions.assertEquals(2, transformedMediaEntityList.size());

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithOwnerCaseInsensitivePrefix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            null, null, null,
            TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryStringPrefix(Integer.toString(nameMatchIndex).toLowerCase(Locale.getDefault())));
        Assertions.assertEquals(2, transformedMediaEntityList.size());

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));

    }

    @Test
    void testFindTranscriptionDocumentWithOwnerPostfix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            null, null, null,  TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryStringPostfix(Integer.toString(nameMatchIndex)));
        Assertions.assertEquals(2, transformedMediaEntityList.size());

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithOwnerCaseInsensitivePostfix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            null, null, null,
            TranscriptionDocumentSubStringQueryEnum.OWNER
                .getQueryStringPostfix(Integer.toString(nameMatchIndex).toLowerCase(Locale.getDefault())));
        Assertions.assertEquals(2, transformedMediaEntityList.size());

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));

    }

    @Test
    void testFindTranscriptionDocumentWithOwnerSubstringMatchAll() {
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            null, null, null,  TranscriptionDocumentSubStringQueryEnum.OWNER.getQueryStringPrefix());
        Assertions.assertEquals(4, transformedMediaEntityList.size());
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(1))));

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(2),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(3),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(1))));
 }

    @Test
    void testFindTranscriptionDocumentWithRequestedByExact() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(null,
                                                                     null, null,
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryString(Integer.toString(nameMatchIndex)), null, null, null, null);

        Assertions.assertEquals(2, transformedMediaEntityList.size());

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedByPrefix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(null,
                                                                     null, null,
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryStringPrefix(Integer.toString(nameMatchIndex)), null, null, null, null);
        Assertions.assertEquals(2, transformedMediaEntityList.size());

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));

    }

    @Test
    void testFindTranscriptionDocumentWithRequestedByCaseInsensitivePrefix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(null,
                                                                     null,  null,
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryStringPrefix(Integer
                                                                                          .toString(nameMatchIndex))
                                                                         .toUpperCase(Locale.getDefault()), null, null, null, null);

        Assertions.assertEquals(2, transformedMediaEntityList.size());

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedByPostfix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,  null, null,
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(Integer.toString(1)), null, null, null, null);
        Assertions.assertEquals(2, transformedMediaEntityList.size());

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));

    }

    @Test
    void testFindTranscriptionDocumentWithRequestedByCaseInsensitivePostfix() {
        int nameMatchIndex = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,  null, null,
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY
                .getQueryStringPostfix(Integer.toString(1).toLowerCase(Locale.getDefault())), null, null, null, null);
        Assertions.assertEquals(2, transformedMediaEntityList.size());

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(nameMatchIndex),
                                                                   generatedDocumentEntities.get(nameMatchIndex).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedBySubstringMatchAll() {
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,  null, null,
            TranscriptionDocumentSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(), null, null, null, null);

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(1))));

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(2),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(3),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedAtFromAndRequestedAtToSameDay() {
        int fromAtPosition = 0;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null, null, null, null,
            generatedDocumentEntities.get(fromAtPosition).getTranscription().getCreatedDateTime(),
            generatedDocumentEntities.get(fromAtPosition).getTranscription().getCreatedDateTime(), null, null);
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedAtFrom() {
        int fromAtPosition = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(
            null,  null, null,
            null, generatedDocumentEntities.get(fromAtPosition).getTranscription().getCreatedDateTime(), null, null, null);
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithRequestedAtTo() {
        int toPosition = 1;
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository.findTranscriptionMedia(null,
                                                              null, null,
                                                                     null, null,
                                                                     generatedDocumentEntities.get(toPosition)
                                                                         .getTranscription().getCreatedDateTime(), null, null);
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(0),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(1),
                                                 getExpectedResult(generatedDocumentEntities.get(0),
                                                                   generatedDocumentEntities.get(0).getTranscription().getCourtCases().get(1))));

        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(2),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(0))));
        Assertions.assertTrue(assertResultEquals(transformedMediaEntityList.get(3),
                                                 getExpectedResult(generatedDocumentEntities.get(1),
                                                                   generatedDocumentEntities.get(1).getTranscription().getCourtCases().get(1))));
    }

    @Test
    void testFindTranscriptionDocumentWithAllQueryParameters() {
        List<TranscriptionDocumentResult> transformedMediaEntityList
            = transcriptionDocumentRepository
            .findTranscriptionMedia(generatedDocumentEntities.get(0).getTranscription()
                                                                  .getCourtCase().getCaseNumber(),
                                    generatedDocumentEntities.get(0).getTranscription().getCourtroom().getCourthouse().getDisplayName(),
                                                                      generatedDocumentEntities.get(0).getTranscription().getHearings().get(0).getHearingDate(),
                                                                      generatedDocumentEntities.get(0).getTranscription().getCreatedBy().getUserFullName(),
                                                                      generatedDocumentEntities.get(0).getTranscription().getCreatedDateTime(),
                                                                      generatedDocumentEntities.get(0).getTranscription().getCreatedDateTime(), false,
                                                                      generatedDocumentEntities.get(0).getTranscription().getTranscriptionWorkflowEntities()
                                        .get(0).getWorkflowActor().getUserFullName());
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedDocumentEntities.get(0).getId(),
                                transformedMediaEntityList.get(0).transcriptionDocumentId());
    }

    private boolean assertResultEquals(TranscriptionDocumentResult asserted, TranscriptionDocumentResult expected) {
        Assertions.assertEquals(expected.transcriptionDocumentId(), asserted.transcriptionDocumentId());
        Assertions.assertEquals(expected.transcriptionId(), asserted.transcriptionId());
        Assertions.assertEquals(expected.caseNumber(), asserted.caseNumber());
        Assertions.assertEquals(expected.caseId(), asserted.caseId());
        Assertions.assertEquals(expected.courthouseId(), asserted.courthouseId());
        Assertions.assertEquals(expected.courthouseDisplayName(), asserted.courthouseDisplayName());
        Assertions.assertEquals(expected.hearingCourthouseId(), asserted.hearingCourthouseId());
        Assertions.assertEquals(expected.hearingCourthouseDisplayName(), asserted.hearingCourthouseDisplayName());
        Assertions.assertEquals(expected.hearingId(), asserted.hearingId());
        Assertions.assertEquals(expected.hearingDate(), asserted.hearingDate());
        Assertions.assertEquals(expected.isManualTranscription(), asserted.isManualTranscription());
        Assertions.assertEquals(expected.isHidden(), asserted.isHidden());
        return true;
    }

    private TranscriptionDocumentResult getExpectedResult(TranscriptionDocumentEntity transformedMediaEntity, CourtCaseEntity caseEntity) {
        return new TranscriptionDocumentResult(transformedMediaEntity.getId(),
                                                                             transformedMediaEntity.getTranscription().getId(),
                                                                             caseEntity != null ? caseEntity.getId() : null,
                                                                             caseEntity != null ? caseEntity.getCaseNumber() : null,
                                                                             transformedMediaEntity.getTranscription().getCourtroom() != null
                                                                                 ? transformedMediaEntity
                                                                                 .getTranscription().getCourtroom().getCourthouse().getId() : null,
                                                                             transformedMediaEntity.getTranscription().getCourtroom() != null
                                                                               ? transformedMediaEntity
                                                                                 .getTranscription().getCourtroom().getCourthouse().getDisplayName() : null,
                                                                             transformedMediaEntity.getTranscription().getHearing() != null
                                                                                 ? transformedMediaEntity
                                                                                 .getTranscription().getHearing().getCourtroom().getCourthouse().getId() : null,
                                                                             transformedMediaEntity.getTranscription().getHearing() != null
                                                                                 ? transformedMediaEntity
                                                                                 .getTranscription().getHearing()
                                                                                 .getCourtroom().getCourthouse().getDisplayName() : null,
                                                                             transformedMediaEntity.getTranscription().getHearing() != null
                                                                                 ? transformedMediaEntity
                                                                                 .getTranscription().getHearing().getId() : null,
                                                                             transformedMediaEntity.getTranscription().getHearing() != null
                                                                                 ? transformedMediaEntity
                                                                                 .getTranscription().getHearing().getHearingDate() : null,
                                                                             transformedMediaEntity.getTranscription().getIsManualTranscription(),
                                                                             transformedMediaEntity.isHidden()
        );
    }
}