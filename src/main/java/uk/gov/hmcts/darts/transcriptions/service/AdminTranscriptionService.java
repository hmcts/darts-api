package uk.gov.hmcts.darts.transcriptions.service;

import uk.gov.hmcts.darts.transcriptions.model.AdminApproveDeletionResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDocumentByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentRequest;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResponse;

import java.time.OffsetDateTime;
import java.util.List;

public interface AdminTranscriptionService {

    List<TranscriptionSearchResponse> searchTranscriptions(TranscriptionSearchRequest request);

    List<SearchTranscriptionDocumentResponse> searchTranscriptionDocument(SearchTranscriptionDocumentRequest request);

    List<GetTranscriptionDetailAdminResponse> getTranscriptionsForUser(Integer userId, OffsetDateTime requestedAtFrom);

    GetTranscriptionDocumentByIdResponse getTranscriptionDocumentById(Long transcriptionDocument);

    TranscriptionDocumentHideResponse hideOrShowTranscriptionDocumentById(Long transcriptionDocumentId,
                                                                          TranscriptionDocumentHideRequest transcriptionDocumentHideRequest);

    AdminApproveDeletionResponse approveDeletionOfTranscriptionDocumentById(Long transcriptionDocumentId);
}