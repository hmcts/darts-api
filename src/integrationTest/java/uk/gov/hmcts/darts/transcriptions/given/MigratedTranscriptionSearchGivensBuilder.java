package uk.gov.hmcts.darts.transcriptions.given;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.UserAccountTestData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.IntStream.range;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;

@Component
@SuppressWarnings("VariableDeclarationUsageDistance")
public class MigratedTranscriptionSearchGivensBuilder extends TranscriptionSearchGivensBuilder {

    @Override
    public void allForHearingOnDate(List<TranscriptionEntity> transcriptionEntities, LocalDate hearingDate) {
        transcriptionEntities.forEach(t -> {
            t.setHearingDate(hearingDate);
            dartsDatabase.save(t);
        });
    }

    @Override
    public List<TranscriptionEntity> persistedTranscriptionsWithDisplayNames(int quantity, String... displayNames) {
        checkParamsQuantity(quantity,"display names", displayNames.length);

        var transcriptions = persistedTranscriptions(quantity);
        range(0, quantity).forEach(j -> {
            var transcription = transcriptions.get(j);
            var courthouse = transcription.getCourtCase().getCourthouse();
            courthouse.setDisplayName(displayNames[j]);
            dartsDatabase.save(courthouse);
        });

        return transcriptions;
    }

    @Override
    public List<TranscriptionEntity> persistedTranscriptionsForHearingsWithHearingDates(int quantity, LocalDate... hearingDates) {
        checkParamsQuantity(quantity,"hearing dates", hearingDates.length);

        var transcriptions = persistedTranscriptions(quantity);
        range(0, quantity).forEach(i -> {

            var transcription = transcriptions.get(i);
            transcription.setHearingDate(hearingDates[i]);
            dartsDatabase.save(transcription);
        });

        return transcriptions;
    }

    @Override
    public void allAtCourthousesWithDisplayName(List<TranscriptionEntity> transcriptionEntities, String courthouseDisplayName) {
        transcriptionEntities.forEach(t -> {
            var courthouse = t.getCourtCase().getCourthouse();
            courthouse.setDisplayName(courthouseDisplayName);
            dartsDatabase.save(courthouse);
        });
    }

    @Override
    public void allForCaseWithCaseNumber(List<TranscriptionEntity> transcriptionEntities, String caseNumber) {
        transcriptionEntities.forEach(t -> {
            var courtCase = t.getCourtCase();
            courtCase.setCaseNumber(caseNumber);
            dartsDatabase.save(courtCase);
        });
    }

    @Override
    public TranscriptionEntity createTranscription() {
        var transcription = PersistableFactory.getTranscriptionTestData().minimalTranscription();
        UserAccountEntity userAccount = dartsDatabase.save(UserAccountTestData.minimalUserAccount());
        transcription.setCreatedBy(userAccount);
        transcription.setLastModifiedBy(userAccount);
        var courtroom = someMinimalCourtRoom();
        dartsDatabase.save(courtroom.getCourthouse());
        dartsDatabase.save(courtroom);
        transcription.setHearings(new ArrayList<>());
        transcription.setCourtroom(courtroom);
        transcription.setIsCurrent(true);
        dartsDatabase.save(transcription.getCourtCase());
        return dartsDatabase.save(transcription);
    }

    @Override
    public TranscriptionEntity createApprovedTranscription() {
        var transcription = PersistableFactory.getTranscriptionTestData().minimalApprovedTranscription();
        var courtroom = someMinimalCourtRoom();
        dartsDatabase.save(courtroom.getCourthouse());
        dartsDatabase.save(courtroom);
        transcription.setHearings(new ArrayList<>());
        transcription.setCourtroom(courtroom);
        dartsDatabase.save(transcription.getCourtCase());
        transcription.setIsCurrent(true);
        return dartsDatabase.save(transcription);
    }
}
