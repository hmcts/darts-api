package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.darts.test.common.data.ObjectAdminActionTestData.objectAdminActionWithDefaults;
import static uk.gov.hmcts.darts.test.common.data.TranscriptionTestData.minimalTranscription;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class TranscriptionDocumentTestData {

    public static TranscriptionDocumentEntity minimalTranscriptionDocument() {
        var transcriptionDocument = new TranscriptionDocumentEntity();
        setupBidirectionalTranscriptionDocuments(transcriptionDocument);

        transcriptionDocument.setFileName("some-file-name");
        transcriptionDocument.setFileType("some-file-type");
        transcriptionDocument.setFileSize(1024);
        transcriptionDocument.setUploadedBy(minimalUserAccount());
        transcriptionDocument.setUploadedDateTime(OffsetDateTime.now());
        transcriptionDocument.setHidden(false);
        transcriptionDocument.setLastModifiedTimestamp(OffsetDateTime.now());
        transcriptionDocument.setLastModifiedBy(minimalUserAccount());

        return transcriptionDocument;
    }

    public static TranscriptionDocumentEntity transcriptionDocumentWithAdminAction() {
        var transcriptionDocument = minimalTranscriptionDocument();
        transcriptionDocument.setHidden(true);
        transcriptionDocument.setAdminActions(Arrays.asList(objectAdminActionWithDefaults()));
        return transcriptionDocument;
    }

    private static void setupBidirectionalTranscriptionDocuments(TranscriptionDocumentEntity transcriptionDocument) {
        var transcriptionDocList = new ArrayList<TranscriptionDocumentEntity>();
        transcriptionDocList.add(transcriptionDocument);
        var transcription = minimalTranscription();
        transcription.setTranscriptionDocumentEntities(transcriptionDocList);
        transcriptionDocument.setTranscription(transcription);
    }

    public static TranscriptionDocumentEntity complexTranscriptionDocument() {
        TranscriptionDocumentEntity documentEntity = new TranscriptionDocumentEntity();
        documentEntity.setId(1);
        documentEntity.setFileName("dummyFileName.txt");
        documentEntity.setFileType("text/plain");
        documentEntity.setFileSize(1024);
        documentEntity.setUploadedDateTime(OffsetDateTime.now());
        documentEntity.setHidden(false);
        documentEntity.setContentObjectId("dummyContentObjectId");
        documentEntity.setClipId("dummyClipId");
        documentEntity.setChecksum("dummyChecksum");
        documentEntity.setLastModifiedTimestamp(OffsetDateTime.now());

        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setId(1);
        courtroomEntity.setName("Dummy Courtroom");

        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setId(1);
        courthouseEntity.setDisplayName("Dummy Courthouse");
        courtroomEntity.setCourthouse(courthouseEntity);

        TranscriptionEntity transcription = new TranscriptionEntity();
        transcription.setId(1);
        transcription.setIsManualTranscription(true);
        documentEntity.setTranscription(transcription);
        transcription.setCourtroom(courtroomEntity);

        ObjectAdminActionEntity adminActionEntity = new ObjectAdminActionEntity();
        adminActionEntity.setId(1);
        adminActionEntity.setComments("Dummy comments");
        adminActionEntity.setTicketReference("Dummy reference");
        adminActionEntity.setHiddenDateTime(OffsetDateTime.now());
        adminActionEntity.setMarkedForManualDelDateTime(OffsetDateTime.now());
        adminActionEntity.setMarkedForManualDeletion(true);

        UserAccountEntity hiddenBy = new UserAccountEntity();
        hiddenBy.setId(100);
        adminActionEntity.setHiddenBy(hiddenBy);

        UserAccountEntity deletedBy = new UserAccountEntity();
        deletedBy.setId(200);
        adminActionEntity.setMarkedForManualDelBy(deletedBy);

        ObjectHiddenReasonEntity objectHiddenReasonEntity = new ObjectHiddenReasonEntity();
        objectHiddenReasonEntity.setId(200);
        adminActionEntity.setObjectHiddenReason(objectHiddenReasonEntity);

        documentEntity.setAdminActions(List.of(adminActionEntity));

        CourtCaseEntity caseEntity = new CourtCaseEntity();
        caseEntity.setId(1);
        caseEntity.setCaseNumber("Dummy case number");

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setId(1);
        hearingEntity.setCourtroom(courtroomEntity);
        hearingEntity.setCourtCase(caseEntity);

        transcription.setHearings(List.of(hearingEntity));

        return documentEntity;
    }
}