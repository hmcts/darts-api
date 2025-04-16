package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestHearingEntity extends HearingEntity implements DbInsertable<HearingEntity> {

    @lombok.Builder
    public TestHearingEntity(
        Integer id,
        CourtroomEntity courtroom,
        LocalDate hearingDate,
        LocalTime scheduledStartTime,
        Boolean hearingIsActual,
        List<JudgeEntity> judges,
        List<MediaEntity> mediaList,
        List<TranscriptionEntity> transcriptions,
        List<MediaRequestEntity> mediaRequests,
        boolean isNew,
        List<EventEntity> eventList,
        CourtCaseEntity courtCase,
        List<AnnotationEntity> annotations,
        OffsetDateTime createdDateTime,
        Integer createdById,
        OffsetDateTime lastModifiedDateTime,
        Integer lastModifiedById
    ) {
        super();
        // Set parent properties
        setId(id);
        setCourtroom(courtroom);
        setHearingDate(hearingDate);
        setScheduledStartTime(scheduledStartTime);
        setHearingIsActual(hearingIsActual);
        setJudges(judges != null ? judges : new ArrayList<>());
        setMediaList(mediaList != null ? mediaList : new ArrayList<>());
        setTranscriptions(transcriptions != null ? transcriptions : new ArrayList<>());
        setMediaRequests(mediaRequests != null ? mediaRequests : new ArrayList<>());
        setNew(isNew);
        setEvents(eventList != null ? Set.of(eventList.toArray(new EventEntity[0])) : new HashSet<>());
        setCourtCase(courtCase);
        setAnnotations(annotations != null ? annotations : new ArrayList<>());
        setCreatedDateTime(createdDateTime);
        setCreatedById(createdById);
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedById(lastModifiedById);
    }

    @Override
    public HearingEntity getEntity() {
        try {
            HearingEntity hearingEntity = new HearingEntity();
            BeanUtils.copyProperties(hearingEntity, this);
            return hearingEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestHearingEntityBuilderRetrieve implements BuilderHolder<TestHearingEntity, TestHearingEntity.TestHearingEntityBuilder> {
       private final TestHearingEntity.TestHearingEntityBuilder builder = TestHearingEntity.builder();

        @Override
        public TestHearingEntity build() {
            return builder.build();
        }

        @Override
        public TestHearingEntity.TestHearingEntityBuilder getBuilder() {
            return builder;
        }
    }
}