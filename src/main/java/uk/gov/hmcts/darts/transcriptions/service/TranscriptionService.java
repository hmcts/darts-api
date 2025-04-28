package uk.gov.hmcts.darts.transcriptions.service;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.transcriptions.model.AdminMarkedForDeletionResponseItem;
import uk.gov.hmcts.darts.transcriptions.model.AttachTranscriptResponse;
import uk.gov.hmcts.darts.transcriptions.model.DownloadTranscriptResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionWorkflowsResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetYourTranscriptsResponse;
import uk.gov.hmcts.darts.transcriptions.model.RequestTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriberViewSummary;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionStatus;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTranscriberCountsResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionsItem;

import java.util.List;

public interface TranscriptionService {

    RequestTranscriptionResponse saveTranscriptionRequest(TranscriptionRequestDetails transcriptionRequestDetails,
                                                          boolean isManual);

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    UpdateTranscriptionResponse updateTranscription(Long transcriptionId,
                                                    UpdateTranscriptionRequest updateTranscription, Boolean allowSelfApprovalOrRejection);

    UpdateTranscriptionAdminResponse updateTranscriptionAdmin(Long transcriptionId,
                                                              UpdateTranscriptionRequest updateTranscription, Boolean allowSelfApprovalOrRejection);

    List<TranscriptionTypeResponse> getTranscriptionTypes();

    List<TranscriptionUrgencyResponse> getTranscriptionUrgenciesByDisplayState();

    GetTranscriptionByIdResponse getTranscription(Long transcriptionId);

    AttachTranscriptResponse attachTranscript(Long transcriptionId, MultipartFile transcript);

    DownloadTranscriptResponse downloadTranscript(Long transcriptionId);

    GetYourTranscriptsResponse getYourTranscripts(Integer userId, Boolean includeHiddenFromRequester);

    List<TranscriberViewSummary> getTranscriberTranscripts(Integer userId, Boolean assigned);

    TranscriptionTranscriberCountsResponse getTranscriptionTranscriberCounts(Integer userId);

    List<UpdateTranscriptionsItem> updateTranscriptions(List<UpdateTranscriptionsItem> request);

    List<TranscriptionStatusEntity> getFinishedTranscriptionStatuses();

    List<TranscriptionStatus> getTranscriptionStatuses();

    void closeTranscription(Long transcriptionId, String transcriptionComment);

    List<TranscriptionDocumentEntity> getAllCaseTranscriptionDocuments(Integer caseId);

    List<GetTranscriptionWorkflowsResponse> getTranscriptionWorkflows(Long transcriptionId, Boolean isCurrent);

    TranscriptionWorkflowEntity saveTranscriptionWorkflow(UserAccountEntity userAccount,
                                                                 TranscriptionEntity transcription,
                                                                 TranscriptionStatusEntity transcriptionStatus,
                                                                 String workflowComment);

    List<Long> rollbackUserTranscriptions(UserAccountEntity entity);

    List<AdminMarkedForDeletionResponseItem> adminGetTranscriptionDocumentsMarkedForDeletion();

}