package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;

import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createSomeMinimalCase;
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
        return minimalTranscription;
    }

    public static TranscriptionEntity someTranscriptionForHearing(HearingEntity hearingEntity) {
        var someTranscription = minimalTranscription();
        someTranscription.addHearing(hearingEntity);
        return someTranscription;
    }

    public static TranscriptionEntity someTranscriptionForCase(CourtCaseEntity courtCaseEntity) {
        var someTranscription = minimalTranscription();
        someTranscription.addCase(courtCaseEntity);
        return someTranscription;
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
