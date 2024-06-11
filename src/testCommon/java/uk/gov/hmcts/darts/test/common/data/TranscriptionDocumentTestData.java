package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
        transcriptionDocument.setAdminActions(List.of(objectAdminActionWithDefaults()));
        return transcriptionDocument;
    }

    private static void setupBidirectionalTranscriptionDocuments(TranscriptionDocumentEntity transcriptionDocument) {
        var transcriptionDocList = new ArrayList<TranscriptionDocumentEntity>();
        transcriptionDocList.add(transcriptionDocument);
        var transcription = minimalTranscription();
        transcription.setTranscriptionDocumentEntities(transcriptionDocList);
        transcriptionDocument.setTranscription(transcription);
    }

}
