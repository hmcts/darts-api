package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.transcriptions.controller.AdminTranscriptionSearchService;
import uk.gov.hmcts.darts.transcriptions.controller.AdminTranscriptionSearchServiceImpl;
import uk.gov.hmcts.darts.transcriptions.controller.TranscriptionSearchQuery;
import uk.gov.hmcts.darts.transcriptions.controller.TranscriptionSearchResult;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
        when(transcriptionSearchQuery.findTranscriptionsCurrentlyOwnedBy("some-owner")).thenReturn(List.of());

        var results = adminTranscriptionSearchService.searchTranscriptions(
            new TranscriptionSearchRequest().owner("some-owner"));

        assertThat(results).isEmpty();
        verifyNoMoreInteractions(transcriptionSearchQuery);
    }

    @Test
    void returnsEmptyIfOwnedByFilterResultsDontIntersectWithProvidedTranscriptionIdFilter() {
        when(transcriptionSearchQuery.findTranscriptionsCurrentlyOwnedBy("some-owner")).thenReturn(List.of(2, 3, 4));

        var results = adminTranscriptionSearchService.searchTranscriptions(
            new TranscriptionSearchRequest()
                .owner("some-owner")
                .transcriptionId(1));

        assertThat(results).isEmpty();
        verifyNoMoreInteractions(transcriptionSearchQuery);
    }

    @Test
    void mapsTranscriptionsSearchResultsToTranscriptionSearchResponse() {
        when(transcriptionSearchQuery.searchTranscriptions(any(TranscriptionSearchRequest.class), anyList()))
            .thenReturn(someSetOfTranscriptionSearchResult(3));

        var results = adminTranscriptionSearchService.searchTranscriptions(new TranscriptionSearchRequest());

        assertThat(results).isEmpty();
        verifyNoMoreInteractions(transcriptionSearchQuery);
    }

    private static Set<TranscriptionSearchResult> someSetOfTranscriptionSearchResult(int quantity) {
        return Set.of(new TranscriptionSearchResult(
            1,
            "some-case-number",
            11,
            LocalDate.parse("2020-01-01"),
            OffsetDateTime.parse("2021-02-02T00:00:00Z"),
            111,
            true));
    }
}