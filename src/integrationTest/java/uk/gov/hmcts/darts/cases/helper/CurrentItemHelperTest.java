package uk.gov.hmcts.darts.cases.helper;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CurrentItemHelperTest extends IntegrationBase {

    @Autowired
    private DartsPersistence dartsPersistence;
    @Autowired
    private CurrentItemHelper currentItemHelper;
    @Autowired
    private CaseService caseService;

    @Test
    void getCurrentEvents_shouldReturnCurrentEvents_whenCurrentandNonCurrentEventsExist() {

        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsPersistence.save(hearing);
        EventEntity eventEntity1 = PersistableFactory.getEventTestData().someMinimalBuilderHolder()
            .getBuilder()
            .hearingEntities(Set.of(hearing))
            .build()
            .getEntity();
        dartsPersistence.save(eventEntity1);

        EventEntity eventEntity2 = PersistableFactory.getEventTestData().someMinimalBuilderHolder()
            .getBuilder()
            .hearingEntities(Set.of(hearing))
            .build()
            .getEntity();
        dartsPersistence.save(eventEntity2);

        EventEntity eventEntity3 = PersistableFactory.getEventTestData().someMinimalBuilderHolder()
            .getBuilder()
            .hearingEntities(Set.of(hearing))
            .isCurrent(false)
            .build()
            .getEntity();
        dartsPersistence.save(eventEntity3);

        dartsPersistence.save(hearing);

        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimalBuilderHolder()
            .getBuilder()
            .hearings(List.of(hearing))
            .build()
            .getEntity();
        dartsPersistence.getCaseRepository().save(courtCaseEntity);

        List<EventEntity> currentEvents = currentItemHelper.getCurrentEvents(courtCaseEntity);
        assertEquals(2, currentEvents.size());
        assertTrue(currentEvents.contains(eventEntity1));
        assertTrue(currentEvents.contains(eventEntity2));
        assertFalse(currentEvents.contains(eventEntity3));
    }

    @Test
    void getCurrentMedia_shouldReturnCurrentMedia_whenCurrentandNonCurrentMediaExists() {

        CourtroomEntity existingCourtroom = PersistableFactory.getCourtroomTestData().someMinimalBuilderHolder().getBuilder()
            .courthouse(PersistableFactory.getCourthouseTestData().someMinimal())
            .build()
            .getEntity();
        dartsPersistence.save(existingCourtroom);

        MediaEntity mediaEntity1 = PersistableFactory.getMediaTestData().someMinimalBuilderHolder()
            .getBuilder()
            .courtroom(existingCourtroom)
            .mediaFile("test")
            .isCurrent(true)
            .build()
            .getEntity();

        dartsPersistence.save(mediaEntity1);
        mediaEntity1.setChronicleId(mediaEntity1.getId().toString());
        dartsPersistence.save(mediaEntity1);

        MediaEntity mediaEntity2 = PersistableFactory.getMediaTestData().someMinimalBuilderHolder()
            .getBuilder()
            .courtroom(existingCourtroom)
            .mediaFile("test")
            .isCurrent(true)
            .build()
            .getEntity();

        dartsPersistence.save(mediaEntity2);
        mediaEntity2.setChronicleId(mediaEntity2.getId().toString());
        dartsPersistence.save(mediaEntity2);

        MediaEntity mediaEntity3 = PersistableFactory.getMediaTestData().someMinimalBuilderHolder()
            .getBuilder()
            .courtroom(existingCourtroom)
            .mediaFile("test")
            .isCurrent(false)
            .build()
            .getEntity();

        dartsPersistence.save(mediaEntity3);
        mediaEntity3.setChronicleId(mediaEntity3.getId().toString());
        dartsPersistence.save(mediaEntity3);

        HearingEntity hearing = PersistableFactory.getHearingTestData().hearingWith(
            "1078",
            "a_courthouse",
            "1",
            LocalDateTime.now().minusYears(7).toString()
        );
        hearing.addMedia(mediaEntity1);
        hearing.addMedia(mediaEntity2);
        hearing.addMedia(mediaEntity3);
        dartsPersistence.save(hearing);

        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimalBuilderHolder()
            .getBuilder()
            .hearings(List.of(hearing))
            .build()
            .getEntity();
        dartsPersistence.getCaseRepository().save(courtCaseEntity);

        List<MediaEntity> currentMedia = currentItemHelper.getCurrentMedia(courtCaseEntity);
        assertEquals(2, currentMedia.size());
        assertTrue(currentMedia.contains(mediaEntity1));
        assertTrue(currentMedia.contains(mediaEntity2));
        assertFalse(currentMedia.contains(mediaEntity3));
    }
}
