package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.test.common.data.builder.CustomTranscriptionEntity;

import java.util.Arrays;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SENTENCING_REMARKS;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class TranscriptionTestData implements Persistable<CustomTranscriptionEntity.CustomTranscriptionEntityBuilderRetrieve> {

    TranscriptionTestData() {
    }

    public TranscriptionEntity minimalTranscription() {
        return someMinimal().build().getEntity();
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
    public CustomTranscriptionEntity.CustomTranscriptionEntityBuilderRetrieve someMinimal() {
        CustomTranscriptionEntity.CustomTranscriptionEntityBuilderRetrieve builder = new CustomTranscriptionEntity.CustomTranscriptionEntityBuilderRetrieve();
        var userAccount = minimalUserAccount();

        var someMinimalCase = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();
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
    public CustomTranscriptionEntity.CustomTranscriptionEntityBuilderRetrieve someMaximal() {
        return someMinimal();
    }
}