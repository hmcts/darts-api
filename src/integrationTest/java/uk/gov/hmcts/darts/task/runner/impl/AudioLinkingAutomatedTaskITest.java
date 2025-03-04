package uk.gov.hmcts.darts.task.runner.impl;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.event.enums.EventStatus;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType.AUDIO_LINKING_TASK;

@DisplayName("AudioLinkingAutomatedTask test")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class AudioLinkingAutomatedTaskITest extends PostgresIntegrationBase {

    private final AudioLinkingAutomatedTask audioLinkingAutomatedTask;
    private static int caseNumberIterator;
    private static final int AUTOMATION_USER_ID = -32;

    @Test
    void positiveMixOfValidMediaAndInvalidMedia() {
        // Enable the task (Can be removed once task is enabled by default)
        AutomatedTaskEntity automatedTask = dartsDatabase.getAutomatedTaskRepository()
            .findByTaskName(AutomatedTaskName.AUDIO_LINKING_TASK_NAME.getTaskName()).orElseThrow();
        automatedTask.setTaskEnabled(true);
        dartsDatabase.getAutomatedTaskRepository().save(automatedTask);

        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");

        MediaEntity media1 = dartsDatabase.getMediaStub().createMediaEntity(
            courtCaseEntity.getCourthouse(), "room 1", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), 1);
        MediaEntity media2 = dartsDatabase.getMediaStub().createMediaEntity(
            courtCaseEntity.getCourthouse(), "room 1", OffsetDateTime.now().plusMinutes(10), OffsetDateTime.now().plusHours(2), 1);

        MediaEntity media3 = dartsDatabase.getMediaStub().createMediaEntity(
            courtCaseEntity.getCourthouse(), "room 1", OffsetDateTime.now().plusHours(2), OffsetDateTime.now().plusHours(4), 1);
        MediaEntity media4 = dartsDatabase.getMediaStub().createMediaEntity(
            courtCaseEntity.getCourthouse(), "room 1", OffsetDateTime.now().plusHours(3), OffsetDateTime.now().plusHours(5), 1);

        HearingEntity hearing1 = createHearing();
        HearingEntity hearing2 = createHearing();
        HearingEntity hearing3 = createHearing();

        CourtroomEntity courtroomEntity = dartsDatabase.createCourtroomUnlessExists("Bristol", "room 1");
        EventEntity event1 = createEvent(EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED, courtroomEntity, hearing1, OffsetDateTime.now());
        EventEntity event2 = createEvent(EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED, courtroomEntity, hearing2, OffsetDateTime.now().plusMinutes(20));
        EventEntity event3 = createEvent(EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED, courtroomEntity, hearing3, OffsetDateTime.now().plusHours(2));

        audioLinkingAutomatedTask.preRunTask();
        audioLinkingAutomatedTask.run();


        transactionalUtil.executeInTransaction(() -> {
            assertEvent(event1, EventStatus.AUDIO_LINKED);
            assertEvent(event2, EventStatus.AUDIO_LINKED);
            assertEvent(event3, EventStatus.AUDIO_LINKED);

            assertMediaLinked(hearing1, media1);
            assertMediaLinked(hearing2, media1);
            assertMediaNotLinked(hearing3, media1);

            assertMediaNotLinked(hearing1, media2);
            assertMediaLinked(hearing2, media2);
            assertMediaNotLinked(hearing3, media2);

            assertMediaNotLinked(hearing1, media3);
            assertMediaNotLinked(hearing2, media3);
            assertMediaLinked(hearing3, media3);

            assertMediaNotLinked(hearing1, media4);
            assertMediaNotLinked(hearing2, media4);
            assertMediaNotLinked(hearing3, media4);
        });
    }

    private void assertMediaNotLinked(HearingEntity hearingEntity, MediaEntity mediaEntity) {
        MediaEntity media = dartsDatabase.getMediaRepository().getReferenceById(mediaEntity.getId());
        HearingEntity hearing = dartsDatabase.getHearingRepository().getReferenceById(hearingEntity.getId());

        assertThat(hearing.containsMedia(mediaEntity)).isFalse();
        assertThat(dartsDatabase.getMediaLinkedCaseRepository().existsByMediaAndCourtCase(media, hearing.getCourtCase())).isFalse();
    }

    private void assertMediaLinked(HearingEntity hearingEntity, MediaEntity mediaEntity) {
        MediaEntity media = dartsDatabase.getMediaRepository().getReferenceById(mediaEntity.getId());
        HearingEntity hearing = dartsDatabase.getHearingRepository().getReferenceById(hearingEntity.getId());

        assertThat(hearing.containsMedia(mediaEntity)).isTrue();
        assertThat(dartsDatabase.getMediaLinkedCaseRepository().existsByMediaAndCourtCase(media, hearing.getCourtCase())).isTrue();
        List<MediaLinkedCaseEntity> mediaLinkedCaseEntities = dartsDatabase.getMediaLinkedCaseRepository().findByMedia(media);
        assertThat(mediaLinkedCaseEntities)
            .allMatch(mediaLinkedCaseEntity -> mediaLinkedCaseEntity.getSource() == AUDIO_LINKING_TASK)
            .allMatch(mediaLinkedCaseEntity -> mediaLinkedCaseEntity.getCreatedBy().getId() == AUTOMATION_USER_ID);
    }

    private EventEntity createEvent(EventStatus eventStatus, CourtroomEntity courtroomEntity, HearingEntity hearing, OffsetDateTime timestamp) {
        EventEntity event = dartsDatabase.createEvent(hearing);
        event.setCourtroom(courtroomEntity);
        event.setEventStatus(eventStatus.getStatusNumber());
        event.setTimestamp(timestamp);
        return dartsDatabase.getEventRepository().save(event);
    }


    private void assertEvent(EventEntity event, EventStatus eventStatus) {
        EventEntity actualEvent = dartsDatabase.getEventRepository().getReferenceById(event.getId());
        assertThat(actualEvent.getEventStatus()).isEqualTo(eventStatus.getStatusNumber());
        assertThat(actualEvent.getLastModifiedBy().getId()).isEqualTo(AUTOMATION_USER_ID);
    }


    private HearingEntity createHearing() {
        return dartsDatabase.createHearing("NEWCASTLE", "Int Test Courtroom 2", String.valueOf(caseNumberIterator++), LocalDateTime.now());
    }
}