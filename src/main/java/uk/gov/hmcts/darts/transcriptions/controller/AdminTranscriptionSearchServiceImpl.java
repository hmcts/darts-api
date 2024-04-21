package uk.gov.hmcts.darts.transcriptions.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.*;
import static java.util.Comparator.comparing;

@Service
@RequiredArgsConstructor
public class AdminTranscriptionSearchServiceImpl implements AdminTranscriptionSearchService {

    private final TranscriptionSearchQuery transcriptionSearchQuery;
    @Override
    public List<TranscriptionSearchResponse> searchTranscriptions(TranscriptionSearchRequest request) {
        var transcriptionSearchResults = transcriptionSearchQuery.searchLegacyTranscriptions(request);
        var nonLegacyTranscriptions = transcriptionSearchQuery.searchNonLegacyTranscriptions(request);

        transcriptionSearchResults.addAll(nonLegacyTranscriptions);

        return transcriptionSearchResults.stream()
            .map(this::toTranscriptionSearchResponse)
            .toList();

    }

    private TranscriptionSearchResponse toTranscriptionSearchResponse(TranscriptionSearchResult transcriptionSearchResult) {
        var transcriptionSearchResponse = new TranscriptionSearchResponse();
        transcriptionSearchResponse.setTranscriptionId(transcriptionSearchResult.id());
        transcriptionSearchResponse.setCaseNumber(transcriptionSearchResult.caseNumber()); // TODO: expand?
        transcriptionSearchResponse.setCourthouseId(transcriptionSearchResult.courthouseId());
        transcriptionSearchResponse.setHearingDate(transcriptionSearchResult.hearingDate());
        transcriptionSearchResponse.setRequestedAt(transcriptionSearchResult.requestedAt());
        transcriptionSearchResponse.setIsManualTranscription(transcriptionSearchResult.isManualTranscription());
        transcriptionSearchResponse.setTranscriptionStatusId(transcriptionSearchResult.transcriptionStatusId());
        return transcriptionSearchResponse;
    }
}
