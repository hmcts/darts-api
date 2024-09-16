package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.test.common.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class EventEntityTest {

    @Test
    void positiveAnonymize() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("event text");

        UserAccountEntity userAccount = new UserAccountEntity();
        eventEntity.anonymize(userAccount);
        assertThat(eventEntity.getEventText()).matches(TestUtils.UUID_REGEX);
        assertThat(eventEntity.isDataAnonymised()).isFalse();//This is only set for manual anonymization
    }
}
