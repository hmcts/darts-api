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
import java.util.Collection;
import java.util.HashSet;

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
        Collection<JudgeEntity> judges,
        Collection<MediaEntity> mediaList,
        Collection<TranscriptionEntity> transcriptions,
        Collection<MediaRequestEntity> mediaRequests,
        boolean isNew,
        Collection<EventEntity> eventList,
        CourtCaseEntity courtCase,
        Collection<AnnotationEntity> annotations,
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
        setJudges(judges != null ? new HashSet<>(judges) : new HashSet<>());
        setMedias(mediaList != null ? new HashSet<>(mediaList) : new HashSet<>());
        setTranscriptions(transcriptions != null ? new HashSet<>(transcriptions) : new HashSet<>());
        setMediaRequests(mediaRequests != null ? new HashSet<>(mediaRequests) : new HashSet<>());
        setNew(isNew);
        setEvents(eventList != null ? new HashSet<>(eventList) : new HashSet<>());
        setCourtCase(courtCase);
        setAnnotations(annotations != null ? new HashSet<>(annotations) : new HashSet<>());
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