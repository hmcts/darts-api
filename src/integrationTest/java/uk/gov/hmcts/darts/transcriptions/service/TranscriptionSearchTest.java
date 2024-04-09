package uk.gov.hmcts.darts.transcriptions.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.transcriptions.controller.AdminTranscriptionSearchService;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static java.time.LocalDate.*;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("VariableDeclarationUsageDistance")
class TranscriptionSearchTest extends IntegrationBase {

    @Autowired
    private TranscriptionSearchGivensBuilder given;

    @Autowired
    private AdminTranscriptionSearchService adminTranscriptionSearchService;

    @Test
    void findsTranscriptionByIdOnly() {
        var persistedTranscriptions = given.persistedTranscriptions(3);
        var transcription = persistedTranscriptions.get(0);

        var transcriptionSearchRequest = new TranscriptionSearchRequest()
            .transcriptionId(transcription.getId());

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(transcription.getId());
    }

    @Test
    void findsSingleTranscriptionByCaseNumberOnly() {
        var persistedTranscriptions = given.persistedTranscriptions(3);
        var transcription = persistedTranscriptions.get(0);

        var transcriptionSearchRequest = new TranscriptionSearchRequest()
            .caseNumber(transcription.getHearing().getCourtCase().getCaseNumber());

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(transcription.getId());
    }

    @Test
    void findsSingleTranscriptionByPartialMatchOnCourtHouseDisplayName() {
        var persistedTranscriptions = given.persistedTranscriptionsWithDisplayNames(
            3, "courthouse-1", "courthouse-2", "courthouse-3");
        var transcription = persistedTranscriptions.get(0);

        var transcriptionSearchRequest = new TranscriptionSearchRequest()
            .courthouseDisplayName("1");

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("courthouseId")
            .containsExactly(transcription.getHearing().getCourtroom().getCourthouse().getId());
    }

    @Test
    void findsSingleTranscriptionBasedOnTranscriptionInitiationType() {
        var persistedTranscriptions = given.persistedTranscriptionsWithIsManualTranscription(
            3, true, false, false);
        var transcriptionSearchRequest = new TranscriptionSearchRequest().isManualTranscription(true);

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(persistedTranscriptions.get(0).getId());
    }


    @Test
    void findsSingleTranscriptionByMatchOnHearingDate() {
        var persistedTranscriptions = given.persistedTranscriptionsForHearingsWithHearingDates(
            3,
            now().plusWeeks(1),
            now().plusWeeks(2),
            now().plusWeeks(3));
        var transcription = persistedTranscriptions.get(0);

        var transcriptionSearchRequest = new TranscriptionSearchRequest()
            .hearingDate(transcription.getHearing().getHearingDate());

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(transcription.getId());
    }

    @Test
    void findsSingleTranscriptionsByCurrentOwner() {
        var persistedTranscriptions = given.persistedTranscriptionsWithCurrentOwners(
            3, "Bob", "Eve", "Alice");

        var transcriptionSearchRequest = new TranscriptionSearchRequest().owner("Al");

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(persistedTranscriptions.get(2).getId());
    }

    @Test
    void findsSingleTranscriptionsByRequester() {
        var persistedTranscriptions = given.persistedTranscriptionsWithRequesters(
            3, "Bob", "Eve", "Alice");

        var transcriptionSearchRequest = new TranscriptionSearchRequest().requestedBy("Al");

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(persistedTranscriptions.get(2).getId());
    }

    @Test
    void findsSingleTranscriptionsByRequestedFromDate() {
        var persistedTranscriptions = given.persistedTranscriptionsWithRequestedDates(
            3,
            parse("2020-01-01"),
            parse("2020-02-01"),
            parse("2020-03-01"));

        var transcriptionSearchRequest = new TranscriptionSearchRequest().requestedAtFrom(parse("2020-02-28"));

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(persistedTranscriptions.get(2).getId());
    }

    @Test
    void findsSingleTranscriptionsByRequestedToDate() {
        var persistedTranscriptions = given.persistedTranscriptionsWithRequestedDates(
            3,
            parse("2020-01-01"),
            parse("2020-02-01"),
            parse("2020-03-01"));

        var transcriptionSearchRequest = new TranscriptionSearchRequest().requestedAtTo(parse("2020-01-02"));

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(persistedTranscriptions.get(0).getId());
    }


    @Test
    void filtersCorrectlyWhenAllFiltersProvided_ExceptTranscriptionId() {
        var transcriptions = given.persistedTranscriptions(20);

        //        private String requestedBy;

        given.allHaveManualTranscription(transcriptions.subList(0, 19), true);
        given.allHaveManualTranscription(transcriptions.subList(19, 20), false);

        given.allCreatedBetween(transcriptions.subList(0, 20), parse("2020-01-01"), parse("2020-12-31"));
        given.allCreatedBefore(transcriptions.subList(18, 19), parse("2020-01-01"));
        given.allCreatedAfter(transcriptions.subList(17, 18), parse("2020-12-31"));

        given.allOwnedBy(transcriptions.subList(0, 15), "Alice");
        given.allOwnedBy(transcriptions.subList(15, 17), "Bob");
        given.allOwnedBy(transcriptions.subList(17, 20), "Eve");

        given.allForHearingOnDate(transcriptions.subList(0, 20), parse("2022-01-01"));
        given.allForHearingOnDate(transcriptions.subList(14, 15), parse("2023-02-03"));

        given.allForCaseWithCaseNumber(transcriptions.subList(0, 20), "case-1");
        given.allForCaseWithCaseNumber(transcriptions.subList(13, 14), "case-2");

        given.allForHearingsAtCourthouseWithDisplayName(transcriptions.subList(0, 20), "courthouse-1");
        given.allForHearingsAtCourthouseWithDisplayName(transcriptions.subList(12, 13), "courthouse-2");

        var transcriptionSearchRequest = new TranscriptionSearchRequest()
            .isManualTranscription(true)                        // Should filter out transcription with id: 20
            .requestedAtFrom(parse("2020-01-01"))          // Should filter out transcription with id: 19
            .requestedAtTo(parse("2020-12-31"))            // Should filter out transcription with id: 18
            .owner("e")                                         // Should filter out transcription with id: 16, 17
            .hearingDate(parse("2022-01-01"))              // Should filter out transcription with id: 15
            .caseNumber("case-1")                               // Should filter out transcription with id: 14
            .courthouseDisplayName("1");                        // Should filter out transcription with id: 13

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

}
