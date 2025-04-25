package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class EventEntityTest {

    @Test
    void getLinkedCases_eventLinkedCaseEntitiesIsNull() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventLinkedCaseEntities(null);
        assertThat(eventEntity.getLinkedCases()).isEmpty();
    }

    @Test
    void getLinkedCases_eventLinkedCaseEntitiesIsEmpty() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventLinkedCaseEntities(new ArrayList<>());
        assertThat(eventEntity.getLinkedCases()).isEmpty();
    }

    @Test
    void getLinkedCases_eventLinkedCaseEntitiesIsNotEmptySingle() {
        EventEntity eventEntity = new EventEntity();
        EventLinkedCaseEntity eventLinkedCaseEntity = new EventLinkedCaseEntity();
        CourtCaseEntity caseEntity = new CourtCaseEntity();
        eventLinkedCaseEntity.setCourtCase(caseEntity);
        eventEntity.getEventLinkedCaseEntities().add(eventLinkedCaseEntity);
        assertThat(eventEntity.getLinkedCases()).containsExactly(caseEntity);
    }

    @Test
    void getLinkedCases_eventLinkedCaseEntitiesIsNotEmptyMultiple() {
        EventEntity eventEntity = new EventEntity();
        EventLinkedCaseEntity eventLinkedCaseEntity1 = new EventLinkedCaseEntity();
        CourtCaseEntity caseEntity1 = new CourtCaseEntity();
        eventLinkedCaseEntity1.setCourtCase(caseEntity1);

        EventLinkedCaseEntity eventLinkedCaseEntity2 = new EventLinkedCaseEntity();
        CourtCaseEntity caseEntity2 = new CourtCaseEntity();
        eventLinkedCaseEntity2.setCourtCase(caseEntity2);

        eventEntity.getEventLinkedCaseEntities().add(eventLinkedCaseEntity1);
        eventEntity.getEventLinkedCaseEntities().add(eventLinkedCaseEntity2);
        assertThat(eventEntity.getLinkedCases()).containsExactly(caseEntity1, caseEntity2);
    }

    @Test
    void isCurrent_whenNull_shouldReturnFalse() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setIsCurrent(null);
        assertThat(eventEntity.isCurrent()).isFalse();
    }

    @Test
    void isCurrent_whenFalse_shouldReturnFalse() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setIsCurrent(false);
        assertThat(eventEntity.isCurrent()).isFalse();
    }

    @Test
    void isCurrent_whenTrue_shouldReturnTrue() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setIsCurrent(true);
        assertThat(eventEntity.isCurrent()).isTrue();
    }
}
