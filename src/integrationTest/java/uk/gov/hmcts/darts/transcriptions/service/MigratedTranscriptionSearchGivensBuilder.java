package uk.gov.hmcts.darts.transcriptions.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.generate;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createSomeMinimalHearing;
import static uk.gov.hmcts.darts.testutils.data.TranscriptionTestData.someTranscriptionForHearing;
import static uk.gov.hmcts.darts.testutils.data.TranscriptionWorkflowTestData.workflowForTranscription;

@Component
@SuppressWarnings("VariableDeclarationUsageDistance")
class MigratedTranscriptionSearchGivensBuilder {
    private final Random random = new Random();

    @Autowired
    private DartsDatabaseStub dartsDatabase;

    public void allForHearingOnDate(List<TranscriptionEntity> transcriptionEntities, LocalDate hearingDate) {
        transcriptionEntities.forEach(t -> {
            var hearing = t.getHearing();
            hearing.setHearingDate(hearingDate);
            dartsDatabase.save(hearing);
        });
    }

    public void allOwnedBy(List<TranscriptionEntity> transcriptions, String owner) {
        transcriptions.forEach(t -> {
            var workflow = workflowForTranscription(t);
            workflow.setWorkflowTimestamp(middayToday());

            var workflowActor = workflow.getWorkflowActor();
            workflowActor.setUserFullName(owner);
            dartsDatabase.save(workflowActor);

            t.getTranscriptionWorkflowEntities().add(workflow);
            dartsDatabase.save(t);
        });

    }

    public void allCreatedBetween(List<TranscriptionEntity> transcription, LocalDate startDate, LocalDate endDate) {
        var diffInDays = ChronoUnit.DAYS.between(startDate, endDate);
        transcription.forEach(t -> {
            t.setCreatedDateTime(
                toOffsetDateTimeAtMidnight(startDate).plusDays(random.nextInt(1,(int) diffInDays)));
            dartsDatabase.save(t);
        });
    }

    public void allCreatedAfter(List<TranscriptionEntity> transcription, LocalDate earliestCreationDate) {
        transcription.forEach(t -> {
            t.setCreatedDateTime(
                toOffsetDateTimeAtMidnight(earliestCreationDate).plusDays(random.nextInt(1,10)));
            dartsDatabase.save(t);
        });
    }

    public void allCreatedBefore(List<TranscriptionEntity> transcription, LocalDate latestCreationDate) {
        transcription.forEach(t -> {
            t.setCreatedDateTime(
                toOffsetDateTimeAtMidnight(latestCreationDate).minusDays(random.nextInt(1,10)));
            dartsDatabase.save(t);
        });
    }

    public void allHaveManualTranscription(List<TranscriptionEntity> transcriptions, boolean isManual) {
        transcriptions.forEach(t -> {
            t.setIsManualTranscription(isManual);
            dartsDatabase.save(t);
        });
    }

    public List<TranscriptionEntity> persistedTranscriptionsWithRequestedDates(int quantity, LocalDate... requestedDates) {
        checkParamsQuantity(quantity,"requested dates", requestedDates.length);

        var transcriptions = persistedTranscriptions(quantity);
        range(0, quantity).forEach(i -> {
            var transcription = transcriptions.get(i);
            transcription.setCreatedDateTime(toOffsetDateTimeAtMidnight(requestedDates[i]));
            dartsDatabase.save(transcription);
        });

        return transcriptions;
    }

    public List<TranscriptionEntity> persistedTranscriptionsWithRequesters(int quantity, String... requesterNames) {
        checkParamsQuantity(quantity,"transcription requesters", requesterNames.length);

        var transcriptions = persistedTranscriptions(quantity);
        range(0, quantity).forEach(i -> {
            var transcription = transcriptions.get(i);
            var requester = transcription.getCreatedBy();
            requester.setUserFullName(requesterNames[i]);
            dartsDatabase.save(requester);
        });

        return transcriptions;
    }

    public List<TranscriptionEntity> persistedTranscriptionsWithCurrentOwners(int quantity, String... ownerNames) {
        checkParamsQuantity(quantity,"current owners", ownerNames.length);

        var transcriptions = persistedTranscriptions(quantity);
        range(0, quantity).forEach(i -> {
            var transcription = transcriptions.get(i);
            // create workflows
            range(0, 3).forEach(j -> {
                var workflow = workflowForTranscription(transcription);
                workflow.setWorkflowTimestamp(middayToday().minusDays(j));

                var workflowActor = workflow.getWorkflowActor();
                workflowActor.setUserFullName(ownerNames[i]);
                dartsDatabase.save(workflowActor);

                transcription.getTranscriptionWorkflowEntities().add(workflow);
            });
            dartsDatabase.save(transcription);
        });

        return transcriptions;
    }

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

    public List<TranscriptionEntity> persistedTranscriptionsWithIsManualTranscription(int quantity, Boolean... isManualTranscriptions) {
        checkParamsQuantity(quantity,"manual transcriptions", isManualTranscriptions.length);

        var transcriptions = persistedTranscriptions(quantity);
        range(0, quantity).forEach(i -> {
            var transcription = transcriptions.get(i);
            transcription.setIsManualTranscription(isManualTranscriptions[i]);
            dartsDatabase.save(transcription);
        });

        return transcriptions;
    }

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

    public List<TranscriptionEntity> persistedTranscriptions(int quantity) {
        return generate(this::createTranscription).limit(quantity).toList();
    }

    public void allForHearingsAtCourthouseWithDisplayName(List<TranscriptionEntity> transcriptionEntities, String courthouseDisplayName) {
        transcriptionEntities.forEach(t -> {
            var hearing = t.getHearing();
            var courthouse = hearing.getCourtroom().getCourthouse();
            courthouse.setDisplayName(courthouseDisplayName);
            dartsDatabase.save(courthouse);
        });
    }

    public TranscriptionEntity createTranscription() {
        var hearing = dartsDatabase.save(createSomeMinimalHearing());
        var transcription = someTranscriptionForHearing(hearing);
        dartsDatabase.save(transcription.getCreatedBy());
        return dartsDatabase.save(transcription);
    }

    public void allForCaseWithCaseNumber(List<TranscriptionEntity> transcriptionEntities, String caseNumber) {
        transcriptionEntities.forEach(t -> {
            var courtCase = t.getHearing().getCourtCase();
            courtCase.setCaseNumber(caseNumber);
            dartsDatabase.save(courtCase);
        });
    }

    private OffsetDateTime middayToday() {
        return OffsetDateTime.of(LocalDate.now(), LocalTime.of(12,0), ZoneOffset.UTC);
    }

    private static OffsetDateTime toOffsetDateTimeAtMidnight(LocalDate requestedDates) {
        return OffsetDateTime.of(requestedDates, LocalTime.of(0, 0), ZoneOffset.UTC);
    }

    private void checkParamsQuantity(int expectedQuantity, String paramName, int actualQuantity) {
        if (actualQuantity != expectedQuantity) {
            throw new IllegalArgumentException("Expected " + expectedQuantity + " " + paramName + " but found " + actualQuantity);
        }
    }
}
