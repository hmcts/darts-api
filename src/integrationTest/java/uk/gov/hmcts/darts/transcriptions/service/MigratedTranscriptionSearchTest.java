package uk.gov.hmcts.darts.transcriptions.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.given.MigratedTranscriptionSearchGivensBuilder;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static java.time.LocalDate.now;
import static java.time.LocalDate.parse;
import static org.assertj.core.api.Assertions.assertThat;

class MigratedTranscriptionSearchTest extends IntegrationBase {

    @Autowired
    private MigratedTranscriptionSearchGivensBuilder given;

    @Autowired
    private AdminTranscriptionService adminTranscriptionSearchService;

    public static final OffsetDateTime WORKFLOW_TIMESTAMP = OffsetDateTime.of(2025, 1, 10, 10,
                                                                              0, 0, 0, ZoneOffset.UTC);

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
            .caseNumber(transcription.getCourtCase().getCaseNumber());

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(transcription.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"house-1", "House-1", "HOUSE-1"})
    void findsSingleTranscriptionByPartialMatchOnCourtHouseDisplayName(String courtHouseDisplayNamePattern) {
        var persistedTranscriptions = given.persistedTranscriptionsWithDisplayNames(
            3, "courthouse-1", "courthouse-2", "courthouse-3");
        var transcription = persistedTranscriptions.get(0);

        var transcriptionSearchRequest = new TranscriptionSearchRequest()
            .courthouseDisplayName(courtHouseDisplayNamePattern);

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("courthouseId")
            .containsExactly(transcription.getCourtCase().getCourthouse().getId());
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
    void findsSingleTranscriptionMatchingOnHearingDate() {
        var persistedTranscriptions = given.persistedTranscriptionsForHearingsWithHearingDates(
            3,
            now().plusWeeks(1),
            now().plusWeeks(2),
            now().plusWeeks(3));
        var transcription = persistedTranscriptions.get(0);

        var transcriptionSearchRequest = new TranscriptionSearchRequest()
            .hearingDate(transcription.getHearingDate());

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(transcription.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"al", "AL", "Al"})
    void findsSingleTranscriptionsByCurrentOwner(String ownerPattern) {
        var persistedTranscriptions = given.persistedTranscriptionsWithCurrentOwners(
            3, "Bob", "Eve", "Alice");

        var transcriptionSearchRequest = new TranscriptionSearchRequest().owner(ownerPattern);

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(persistedTranscriptions.get(2).getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"al", "AL", "Al"})
    void findsSingleTranscriptionsByRequester(String requesterPattern) {
        var persistedTranscriptions = given.persistedTranscriptionsWithRequesters(
            3, "Bob", "Eve", "Alice");

        var transcriptionSearchRequest = new TranscriptionSearchRequest().requestedBy(requesterPattern);

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

        given.allHaveManualTranscription(transcriptions.subList(0, 19), true);
        given.allHaveManualTranscription(transcriptions.subList(19, 20), false);

        given.allCreatedBetween(transcriptions.subList(0, 20), parse("2020-01-01"), parse("2020-12-31"));
        given.allCreatedBefore(transcriptions.subList(18, 19), parse("2020-01-01"));
        given.allCreatedAfter(transcriptions.subList(17, 18), parse("2020-12-31"));

        given.allOwnedBy(transcriptions.subList(0, 15), "Alice", TranscriptionStatusEnum.REQUESTED);
        given.allOwnedBy(transcriptions.subList(15, 17), "Bob", TranscriptionStatusEnum.REQUESTED);
        given.allOwnedBy(transcriptions.subList(17, 20), "Eve", TranscriptionStatusEnum.REQUESTED);

        given.allForHearingOnDate(transcriptions.subList(0, 20), parse("2022-01-01"));
        given.allForHearingOnDate(transcriptions.subList(14, 15), parse("2023-02-03"));

        given.allForCaseWithCaseNumber(transcriptions.subList(0, 20), "case-1");
        given.allForCaseWithCaseNumber(transcriptions.subList(13, 14), "case-2");

        given.allAtCourthousesWithDisplayName(transcriptions.subList(0, 20), "courthouse-1");
        given.allAtCourthousesWithDisplayName(transcriptions.subList(12, 13), "courthouse-2");

        var transcriptionSearchRequest = new TranscriptionSearchRequest()
            .isManualTranscription(true)                        // Should filter out transcription at index: 19
            .requestedAtFrom(parse("2020-01-01"))          // Should filter out transcription at index: 18
            .requestedAtTo(parse("2020-12-31"))            // Should filter out transcription at index: 17
            .owner("e")                                         // Should filter out transcription at index: 16, 15
            .hearingDate(parse("2022-01-01"))              // Should filter out transcription at index: 14
            .caseNumber("case-1")                               // Should filter out transcription at index: 13
            .courthouseDisplayName("1");                        // Should filter out transcription at index: 12

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(
                transcriptions.get(11).getId(),
                transcriptions.get(10).getId(),
                transcriptions.get(9).getId(),
                transcriptions.get(8).getId(),
                transcriptions.get(7).getId(),
                transcriptions.get(6).getId(),
                transcriptions.get(5).getId(),
                transcriptions.get(4).getId(),
                transcriptions.get(3).getId(),
                transcriptions.get(2).getId(),
                transcriptions.get(1).getId(),
                transcriptions.get(0).getId());
    }

    @Test
    void searchTranscriptionsReturnsTranscriptionWithApprovedOnDetailsForApprovedTranscription() {
        var transcriptions = given.persistedApprovedTranscriptions(20);

        given.allHaveManualTranscription(transcriptions.subList(0, 19), true);
        given.allHaveManualTranscription(transcriptions.subList(19, 20), false);

        given.allCreatedBetween(transcriptions.subList(0, 20), parse("2020-01-01"), parse("2020-12-31"));
        given.allCreatedBefore(transcriptions.subList(18, 19), parse("2020-01-01"));
        given.allCreatedAfter(transcriptions.subList(17, 18), parse("2020-12-31"));

        given.allOwnedBy(transcriptions.subList(0, 15), "Alice", TranscriptionStatusEnum.APPROVED);
        given.allOwnedBy(transcriptions.subList(15, 17), "Bob", TranscriptionStatusEnum.APPROVED);
        given.allOwnedBy(transcriptions.subList(17, 20), "Eve", TranscriptionStatusEnum.APPROVED);

        given.allForHearingOnDate(transcriptions.subList(0, 20), parse("2022-01-01"));
        given.allForHearingOnDate(transcriptions.subList(14, 15), parse("2023-02-03"));

        given.allForCaseWithCaseNumber(transcriptions.subList(0, 20), "case-1");
        given.allForCaseWithCaseNumber(transcriptions.subList(13, 14), "case-2");

        given.allAtCourthousesWithDisplayName(transcriptions.subList(0, 20), "courthouse-1");
        given.allAtCourthousesWithDisplayName(transcriptions.subList(12, 13), "courthouse-2");

        var transcriptionSearchRequest = new TranscriptionSearchRequest()
            .isManualTranscription(true)                        // Should filter out transcription at index: 19
            .requestedAtFrom(parse("2020-01-01"))          // Should filter out transcription at index: 18
            .requestedAtTo(parse("2020-12-31"))            // Should filter out transcription at index: 17
            .owner("e")                                         // Should filter out transcription at index: 16, 15
            .hearingDate(parse("2022-01-01"))              // Should filter out transcription at index: 14
            .caseNumber("case-1")                               // Should filter out transcription at index: 13
            .courthouseDisplayName("1");                        // Should filter out transcription at index: 12

        var transcriptionResponse = adminTranscriptionSearchService.searchTranscriptions(transcriptionSearchRequest);

        assertThat(transcriptionResponse).extracting("transcriptionId")
            .containsExactly(
                transcriptions.get(11).getId(),
                transcriptions.get(10).getId(),
                transcriptions.get(9).getId(),
                transcriptions.get(8).getId(),
                transcriptions.get(7).getId(),
                transcriptions.get(6).getId(),
                transcriptions.get(5).getId(),
                transcriptions.get(4).getId(),
                transcriptions.get(3).getId(),
                transcriptions.get(2).getId(),
                transcriptions.get(1).getId(),
                transcriptions.get(0).getId());
        assertThat(transcriptionResponse.get(0).getApprovedAt()).isEqualTo(WORKFLOW_TIMESTAMP);
        assertThat(transcriptionResponse.get(1).getApprovedAt()).isEqualTo(WORKFLOW_TIMESTAMP);
        assertThat(transcriptionResponse.get(2).getApprovedAt()).isEqualTo(WORKFLOW_TIMESTAMP);
        assertThat(transcriptionResponse.get(3).getApprovedAt()).isEqualTo(WORKFLOW_TIMESTAMP);
        assertThat(transcriptionResponse.get(4).getApprovedAt()).isEqualTo(WORKFLOW_TIMESTAMP);
        assertThat(transcriptionResponse.get(5).getApprovedAt()).isEqualTo(WORKFLOW_TIMESTAMP);
        assertThat(transcriptionResponse.get(6).getApprovedAt()).isEqualTo(WORKFLOW_TIMESTAMP);
        assertThat(transcriptionResponse.get(7).getApprovedAt()).isEqualTo(WORKFLOW_TIMESTAMP);
        assertThat(transcriptionResponse.get(8).getApprovedAt()).isEqualTo(WORKFLOW_TIMESTAMP);
        assertThat(transcriptionResponse.get(9).getApprovedAt()).isEqualTo(WORKFLOW_TIMESTAMP);
        assertThat(transcriptionResponse.get(10).getApprovedAt()).isEqualTo(WORKFLOW_TIMESTAMP);
        assertThat(transcriptionResponse.get(11).getApprovedAt()).isEqualTo(WORKFLOW_TIMESTAMP);
    }

}