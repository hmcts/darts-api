package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestTranscriptionDocumentEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestTranscriptionEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.darts.test.common.data.ObjectAdminActionTestData.objectAdminActionWithDefaults;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public class TranscriptionDocumentTestData
    implements Persistable<TestTranscriptionDocumentEntity.TranscriptionDocumentEntityBuilderRetrieve,
    TranscriptionDocumentEntity, TestTranscriptionDocumentEntity.TestTranscriptionDocumentEntityBuilder> {

    TranscriptionDocumentTestData() {
    }


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
        transcriptionDocument.setLastModifiedById(0);

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
        var transcription = PersistableFactory.getTranscriptionTestData().minimalTranscription();
        transcription.setTranscriptionDocumentEntities(transcriptionDocList);
        transcriptionDocument.setTranscription(transcription);
    }

    public TestTranscriptionDocumentEntity.TranscriptionDocumentEntityBuilderRetrieve complexTranscriptionDocument() {
        TestTranscriptionDocumentEntity.TranscriptionDocumentEntityBuilderRetrieve builder =
            new TestTranscriptionDocumentEntity.TranscriptionDocumentEntityBuilderRetrieve();

        builder.getBuilder().id(1)
            .fileName("dummyFileName.txt")
            .fileType("text/plain").fileSize(1024)
            .uploadedDateTime(OffsetDateTime.now())
            .isHidden(false)
            .contentObjectId("dummyContentObjectId")
            .clipId("dummyClipId")
            .checksum("dummyChecksum")
            .lastModifiedDateTime(OffsetDateTime.now());

        CourtroomEntity courtroomEntity = CourtroomTestData.someMinimalCourtRoom();
        courtroomEntity.setId(1);
        courtroomEntity.setName("Dummy Courtroom");

        CourthouseEntity courthouseEntity = CourthouseTestData.someMinimalCourthouse();
        courthouseEntity.setId(1);
        courthouseEntity.setDisplayName("Dummy Courthouse");
        courtroomEntity.setCourthouse(courthouseEntity);

        TestTranscriptionEntity.TestTranscriptionEntityBuilderRetrieve
            transcriptionEntityBuilderRetrieve = PersistableFactory.getTranscriptionTestData().someMinimalBuilderHolder();
        transcriptionEntityBuilderRetrieve.getBuilder().id(1)
            .isManualTranscription(true).courtroom(courtroomEntity);

        ObjectAdminActionEntity adminActionEntity = ObjectAdminActionTestData.minimalObjectAdminAction();
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

        builder.getBuilder().adminActions(List.of(adminActionEntity));

        CourtCaseEntity caseEntity = new CourtCaseEntity();
        caseEntity.setId(1);
        caseEntity.setCaseNumber("Dummy case number");

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setId(1);
        hearingEntity.setCourtroom(courtroomEntity);
        hearingEntity.setCourtCase(caseEntity);

        transcriptionEntityBuilderRetrieve.getBuilder().hearings(List.of(hearingEntity)).build();
        builder.getBuilder().transcription(transcriptionEntityBuilderRetrieve.build());

        return builder;
    }

    @Override
    public TranscriptionDocumentEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestTranscriptionDocumentEntity.TranscriptionDocumentEntityBuilderRetrieve someMinimalBuilderHolder() {
        TestTranscriptionDocumentEntity.TranscriptionDocumentEntityBuilderRetrieve builder =
            new TestTranscriptionDocumentEntity.TranscriptionDocumentEntityBuilderRetrieve();

        var transcription = PersistableFactory.getTranscriptionTestData().minimalTranscription();

        builder.getBuilder()
            .fileName("some-file-name")
            .fileType("some-file-type").fileSize(1024)
            .uploadedDateTime(OffsetDateTime.now())
            .uploadedBy(minimalUserAccount())
            .isHidden(false)
            .lastModifiedBy(minimalUserAccount())
            .lastModifiedDateTime(OffsetDateTime.now())
            .lastModifiedDateTime(OffsetDateTime.now())
            .transcription(transcription);

        return builder;
    }

    @Override
    public TestTranscriptionDocumentEntity.TestTranscriptionDocumentEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }
}