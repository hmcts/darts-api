package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestTranscriptionEntity;

import java.util.Arrays;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SENTENCING_REMARKS;

public class TranscriptionTestData
    implements Persistable<TestTranscriptionEntity.TestTranscriptionEntityBuilderRetrieve, TranscriptionEntity,
    TestTranscriptionEntity.TestTranscriptionEntityBuilder> {

    TranscriptionTestData() {
    }

    public TranscriptionEntity minimalTranscription() {
        var minimalTranscription = new TranscriptionEntity();
        var someMinimalCase = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();
        minimalTranscription.addCase(someMinimalCase);
        minimalTranscription.setTranscriptionType(someTranscriptionType());
        minimalTranscription.setTranscriptionStatus(someTranscriptionStatus());
        minimalTranscription.setHideRequestFromRequestor(false);
        minimalTranscription.setIsManualTranscription(false);
        var userAccount = minimalUserAccount();
        minimalTranscription.setLastModifiedBy(userAccount);
        minimalTranscription.setCreatedBy(userAccount);
        return minimalTranscription;
    }

    public TranscriptionTypeEntity someTranscriptionType() {
        var transcriptionType = new TranscriptionTypeEntity();
        transcriptionType.setId(SENTENCING_REMARKS.getId());
        return transcriptionType;
    }

    public TranscriptionStatusEntity someTranscriptionStatus() {
        var transcriptionStatus = new TranscriptionStatusEntity();
        transcriptionStatus.setId(REQUESTED.getId());
        return transcriptionStatus;
    }

    public TranscriptionEntity someTranscriptionForHearing(HearingEntity hearingEntity) {
        var transcription = minimalTranscription();
        transcription.addHearing(hearingEntity);
        transcription.setCourtCases(Arrays.asList(hearingEntity.getCourtCase()));
        return transcription;
    }

    @Override
    public TranscriptionEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestTranscriptionEntity.TestTranscriptionEntityBuilderRetrieve someMinimalBuilderHolder() {
        TestTranscriptionEntity.TestTranscriptionEntityBuilderRetrieve builder = new TestTranscriptionEntity.TestTranscriptionEntityBuilderRetrieve();
        var userAccount = minimalUserAccount();

        builder.getBuilder()
            .transcriptionType(someTranscriptionType())
            .transcriptionStatus(someTranscriptionStatus())
            .hideRequestFromRequestor(false)
            .isManualTranscription(false)
            .lastModifiedBy(userAccount)
            .createdBy(userAccount);

        return builder;
    }

    @Override
    public TestTranscriptionEntity.TestTranscriptionEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }
}