package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.List;

import static java.util.Objects.nonNull;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestEventEntity extends EventEntity implements DbInsertable<EventEntity> {

    @lombok.Builder
    public TestEventEntity(
        Integer id,
        String legacyObjectId,
        EventHandlerEntity eventType,
        Integer eventId,
        String eventText,
        OffsetDateTime timestamp,
        CourtroomEntity courtroom,
        String legacyVersionLabel,
        String messageId,
        boolean isLogEntry,
        String chronicleId,
        String antecedentId,
        List<HearingEntity> hearingEntities,
        Integer eventStatus,
        Boolean isCurrent,
        boolean isDataAnonymised
    ) {
        super();
        setId(id);
        setLegacyObjectId(legacyObjectId);
        setEventType(eventType);
        setEventId(eventId);
        setEventText(eventText);
        setTimestamp(timestamp);
        setCourtroom(courtroom);
        setLegacyVersionLabel(legacyVersionLabel);
        setMessageId(messageId);
        setLogEntry(isLogEntry);
        setChronicleId(chronicleId);
        setAntecedentId(antecedentId);
        setHearingEntities(nonNull(hearingEntities) ? hearingEntities : List.of());
        setEventStatus(eventStatus);
        setIsCurrent(isCurrent);
        setDataAnonymised(isDataAnonymised);
    }

    @Override
    public EventEntity getEntity() {
        try {
            EventEntity eventEntity = new EventEntity();
            BeanUtils.copyProperties(eventEntity, this);
            return eventEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    @NoArgsConstructor
    public static class TestEventEntityBuilderRetrieve
        implements BuilderHolder<TestEventEntity, TestEventEntity.TestEventEntityBuilder> {

        private final TestEventEntity.TestEventEntityBuilder builder = TestEventEntity.builder();

        @Override
        public TestEventEntity build() {
            return builder.build();
        }

        @Override
        public TestEventEntity.TestEventEntityBuilder getBuilder() {
            return builder;
        }
    }
}
