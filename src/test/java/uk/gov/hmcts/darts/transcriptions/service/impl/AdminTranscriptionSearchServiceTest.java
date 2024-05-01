package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResult;
import uk.gov.hmcts.darts.transcriptions.service.AdminTranscriptionSearchService;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionSearchQuery;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTranscriptionSearchServiceTest {

    private AdminTranscriptionSearchService adminTranscriptionSearchService;

    @Mock
    private TranscriptionSearchQuery transcriptionSearchQuery;

    @BeforeEach
    void setUp() {
        adminTranscriptionSearchService = new AdminTranscriptionSearchServiceImpl(transcriptionSearchQuery);
    }

    @Test
    void returnsEmptyIfOwnerFilterProvidedWithNoMatches() {
        when(transcriptionSearchQuery.findTranscriptionsIdsCurrentlyOwnedBy("some-owner")).thenReturn(List.of());

        var results = adminTranscriptionSearchService.searchTranscriptions(
            new TranscriptionSearchRequest().owner("some-owner"));

        assertThat(results).isEmpty();
        verifyNoMoreInteractions(transcriptionSearchQuery);
    }

    @Test
    void returnsEmptyIfOwnedByFilterResultsDontIntersectWithProvidedTranscriptionIdFilter() {
        when(transcriptionSearchQuery.findTranscriptionsIdsCurrentlyOwnedBy("some-owner")).thenReturn(List.of(2, 3, 4));

        var results = adminTranscriptionSearchService.searchTranscriptions(
            new TranscriptionSearchRequest()
                .owner("some-owner")
                .transcriptionId(1));

        assertThat(results).isEmpty();
        verifyNoMoreInteractions(transcriptionSearchQuery);
    }

    @Test
    void mapsTranscriptionsSearchResultsToTranscriptionSearchResponse() {
        var transcriptionSearchResults = someSetOfTranscriptionSearchResult(3);
        when(transcriptionSearchQuery.searchTranscriptions(any(TranscriptionSearchRequest.class), any()))
            .thenReturn(transcriptionSearchResults);

        var searchResponses = adminTranscriptionSearchService.searchTranscriptions(new TranscriptionSearchRequest());

        assertThat(searchResponses).extracting("transcriptionId").containsExactly(1, 2, 3);
        assertThat(searchResponses).extracting("caseNumber").containsExactly("case-number-1", "case-number-2", "case-number-3");
        assertThat(searchResponses).extracting("courthouseId").containsExactly(11, 12, 13);
        assertThat(searchResponses).extracting("hearingDate").containsExactly(
                LocalDate.parse("2020-01-02"),
                LocalDate.parse("2020-01-03"),
                LocalDate.parse("2020-01-04"));
        assertThat(searchResponses).extracting("requestedAt").containsExactly(
                OffsetDateTime.parse("2021-02-03T00:00:00Z"),
                OffsetDateTime.parse("2021-02-04T00:00:00Z"),
                OffsetDateTime.parse("2021-02-05T00:00:00Z"));
        assertThat(searchResponses).extracting("transcriptionStatusId").containsExactly(21, 22, 23);
        assertThat(searchResponses).extracting("isManualTranscription").containsExactly(false, true, false);

        verifyNoMoreInteractions(transcriptionSearchQuery);
    }

    private static Set<TranscriptionSearchResult> someSetOfTranscriptionSearchResult(int quantity) {
        return rangeClosed(1, quantity).mapToObj(i -> createTranscription(i)).collect(toSet());
    }

    private static TranscriptionSearchResult createTranscription(int seed) {
        return new TranscriptionSearchResult(
            seed,
            "case-number-" + seed,
            seed + 10,
            LocalDate.parse("2020-01-01").plusDays(seed),
            OffsetDateTime.parse("2021-02-02T00:00:00Z").plusDays(seed),
            seed + 20,
            seed % 2 == 0);
    }

}