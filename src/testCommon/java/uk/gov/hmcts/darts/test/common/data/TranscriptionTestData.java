package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;

import java.util.Arrays;

import static uk.gov.hmcts.darts.test.common.data.CaseTestData.createSomeMinimalCase;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SENTENCING_REMARKS;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class TranscriptionTestData {

    public static TranscriptionEntity minimalTranscription() {
        var minimalTranscription = new TranscriptionEntity();
        var someMinimalCase = createSomeMinimalCase();
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

    public static TranscriptionEntity someTranscriptionForHearing(HearingEntity hearingEntity) {
        var transcription = minimalTranscription();
        transcription.addHearing(hearingEntity);
        transcription.setCourtCases(Arrays.asList(hearingEntity.getCourtCase()));
        return transcription;
    }

    public static TranscriptionTypeEntity someTranscriptionType() {
        var transcriptionType = new TranscriptionTypeEntity();
        transcriptionType.setId(SENTENCING_REMARKS.getId());
        return transcriptionType;
    }

    public static TranscriptionStatusEntity someTranscriptionStatus() {
        var transcriptionStatus = new TranscriptionStatusEntity();
        transcriptionStatus.setId(REQUESTED.getId());
        return transcriptionStatus;
    }
}
