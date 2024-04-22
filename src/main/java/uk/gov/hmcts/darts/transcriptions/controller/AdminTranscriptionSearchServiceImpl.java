package uk.gov.hmcts.darts.transcriptions.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResponse;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.*;
import static java.util.Comparator.comparing;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class AdminTranscriptionSearchServiceImpl implements AdminTranscriptionSearchService {

    private final TranscriptionSearchQuery transcriptionSearchQuery;
    @Override
    public List<TranscriptionSearchResponse> searchTranscriptions(TranscriptionSearchRequest request) {
        List<Integer> transcriptionsForOwner = new ArrayList<>();
        if (request.getOwner() != null) {
            transcriptionsForOwner = transcriptionSearchQuery.findTranscriptionsCurrentlyOwnedBy(request.getOwner());
            if (transcriptionsForOwner.isEmpty()) {
                return emptyList();
            }
            if (transcriptionIdIsNotOwnedBy(request.getTranscriptionId(), transcriptionsForOwner)) {
                return emptyList();
            }
        }

        var transcriptionIds = new ArrayList<Integer>();
        if (request.getTranscriptionId() != null) {
            transcriptionIds.add(request.getTranscriptionId());
        } else if (!isEmpty(transcriptionsForOwner)) {
            transcriptionIds.addAll(transcriptionsForOwner);
        } else {
            transcriptionIds = null;
        }

        return transcriptionSearchQuery.searchTranscriptions(request, transcriptionIds).stream()
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

    private static boolean transcriptionIdIsNotOwnedBy(Integer transcriptionId, List<Integer> transcriptionsForOwner) {
        return transcriptionId != null
            && !isEmpty(transcriptionsForOwner)
            && !transcriptionsForOwner.contains(transcriptionId);
    }
}
