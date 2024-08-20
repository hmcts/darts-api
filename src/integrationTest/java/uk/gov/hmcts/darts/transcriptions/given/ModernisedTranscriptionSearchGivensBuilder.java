package uk.gov.hmcts.darts.transcriptions.given;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.IntStream.range;
import static uk.gov.hmcts.darts.test.common.data.HearingTestData.someMinimalHearing;
import static uk.gov.hmcts.darts.test.common.data.TranscriptionTestData.someTranscriptionForHearing;

@Component
public class ModernisedTranscriptionSearchGivensBuilder extends TranscriptionSearchGivensBuilder {


    @Override
    public void allForHearingOnDate(List<TranscriptionEntity> transcriptionEntities, LocalDate hearingDate) {
        transcriptionEntities.forEach(t -> {
            var hearing = t.getHearing();
            hearing.setHearingDate(hearingDate);
            dartsDatabase.save(hearing);
        });
    }

    @Override
    public List<TranscriptionEntity> persistedTranscriptionsWithDisplayNames(int quantity, String... displayNames) {
        checkParamsQuantity(quantity,"display names", displayNames.length);

        var transcriptions = persistedTranscriptions(quantity);
        range(0, quantity).forEach(j -> {
            var transcription = transcriptions.get(j);
            var courthouse = transcription.getHearing().getCourtroom().getCourthouse();
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
            var hearing = transcription.getHearing();
            hearing.setHearingDate(hearingDates[i]);
            dartsDatabase.save(hearing);
        });

        return transcriptions;
    }

    @Override
    public void allAtCourthousesWithDisplayName(List<TranscriptionEntity> transcriptionEntities, String courthouseDisplayName) {
        transcriptionEntities.forEach(t -> {
            var hearing = t.getHearing();
            var courthouse = hearing.getCourtroom().getCourthouse();
            courthouse.setDisplayName(courthouseDisplayName);
            dartsDatabase.save(courthouse);
        });
    }


    @Override
    public void allForCaseWithCaseNumber(List<TranscriptionEntity> transcriptionEntities, String caseNumber) {
        transcriptionEntities.forEach(t -> {
            var courtCase = t.getHearing().getCourtCase();
            courtCase.setCaseNumber(caseNumber);
            dartsDatabase.save(courtCase);
        });
    }

    @Override
    public TranscriptionEntity createTranscription() {
        var hearing = dartsDatabase.save(someMinimalHearing());
        var transcription = someTranscriptionForHearing(hearing);
        dartsDatabase.save(transcription.getCreatedBy());
        return dartsDatabase.save(transcription);
    }

}
