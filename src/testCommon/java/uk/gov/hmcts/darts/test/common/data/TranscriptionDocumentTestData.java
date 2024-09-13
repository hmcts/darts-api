package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.builder.CustomTranscriptionDocumentEntity;
import uk.gov.hmcts.darts.test.common.data.builder.CustomTranscriptionEntity;

import java.time.OffsetDateTime;
import java.util.List;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class TranscriptionDocumentTestData implements Persistable<CustomTranscriptionDocumentEntity.TranscriptionDocumentEntityBuilderRetrieve> {

    TranscriptionDocumentTestData() {
    }

    public CustomTranscriptionDocumentEntity.TranscriptionDocumentEntityBuilderRetrieve complexTranscriptionDocument() {
        CustomTranscriptionDocumentEntity.TranscriptionDocumentEntityBuilderRetrieve builder =
            new CustomTranscriptionDocumentEntity.TranscriptionDocumentEntityBuilderRetrieve();

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

        CustomTranscriptionEntity.CustomTranscriptionEntityBuilderRetrieve transcriptionEntityBuilderRetrieve = PersistableFactory.getTranscriptionTestData().someMinimal();
        transcriptionEntityBuilderRetrieve.getBuilder().id(1)
            .isManualTranscription(true).courtroom(courtroomEntity);

        builder.getBuilder().transcription(transcriptionEntityBuilderRetrieve.build());
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

        transcriptionEntityBuilderRetrieve.getBuilder().hearings(List.of(hearingEntity));

        return builder;
    }

    @Override
    public CustomTranscriptionDocumentEntity.TranscriptionDocumentEntityBuilderRetrieve someMinimal() {
        return someMinimal();
    }

    @Override
    public CustomTranscriptionDocumentEntity.TranscriptionDocumentEntityBuilderRetrieve someMaximal() {
        return someMinimal();
    }
}