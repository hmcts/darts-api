package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


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

        assertThat(adminTranscriptionSearchService.searchTranscriptions(
            new TranscriptionSearchRequest().owner("some-owner"))).isEmpty();

        verifyNoMoreInteractions(transcriptionSearchQuery);
    }
}