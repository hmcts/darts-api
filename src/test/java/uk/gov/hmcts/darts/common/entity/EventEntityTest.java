package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventEntityTest {

    @Test
    void positiveAnonymize() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("event text");

        UserAccountEntity userAccount = new UserAccountEntity();
        UUID uuid = UUID.randomUUID();

        eventEntity.anonymize(userAccount, uuid);
        assertThat(eventEntity.getEventText()).isEqualTo(uuid.toString());
        assertThat(eventEntity.isDataAnonymised()).isFalse();//This is only set for manual anonymization
    }
}
